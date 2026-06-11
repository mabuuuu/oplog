package pers.mabu.oplog.aspect;

import com.alibaba.fastjson.JSONObject;
import pers.mabu.oplog.annotation.OpLog;
import pers.mabu.oplog.core.*;
import pers.mabu.oplog.service.IFunctionService;
import pers.mabu.oplog.service.IOpLogRecordService;
import pers.mabu.oplog.service.IOperatorGetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * 操作日志切面
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oplog.enable", havingValue = "true", matchIfMissing = true)
public class OpLogAspect {
    private final OpLogValueParser opLogValueParser;
    private final IFunctionService functionService;
    private final IOperatorGetService operatorGetService;
    private final IOpLogRecordService opLogRecordService;

    @Pointcut("@annotation(pers.mabu.oplog.annotation.OpLog)")
    public void pointcut() {
    }

    @Around(value = "@annotation(opLog)")
    public Object opLogAdvice(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return execute(joinPoint, joinPoint.getTarget(), method, joinPoint.getArgs(), opLog);
    }

    private Object execute(ProceedingJoinPoint joinPoint, Object target, Method method, Object[] args, OpLog opLog) throws Throwable {
        MethodExecuteResult methodExecuteResult = new MethodExecuteResult(true, null, "");
        Object ret = null;
        OpLogEvaluationContext evaluationContext = null;
        List<CustomMethodParams> customMethods = null;
        String logContent = opLog.success();
        boolean condition = true;

        try {
            evaluationContext = opLogValueParser.createEvaluationContext(method, args, target, ret, methodExecuteResult.getErrorMsg());
            if (StringUtils.isNotBlank(opLog.condition())) {
                condition = opLogValueParser.parseExpressionCondition(opLog.condition(), method, target, evaluationContext);
            }
        } catch (Exception e) {
            log.error("处理结果集失败", e);
        }
        if (condition) {
            try {
                customMethods = OpLogRegular.getMethod(opLog.success());
                // 处理前置自定义函数
                if (CollectionUtils.isNotEmpty(customMethods)) {
                    logContent = processExecuteFunctionTemplate(customMethods, logContent, method, target, evaluationContext, true);
                }
            } catch (Exception e) {
                log.error("操作日志前置自定义函数解析失败", e);
            }
        }

        try {
            ret = joinPoint.proceed();
        } catch (Exception e) {
            methodExecuteResult = new MethodExecuteResult(false, e, e.getMessage());
        }

        if (methodExecuteResult.isSuccess() && condition) {
            try {
                executeAfter(joinPoint, target, method, opLog, logContent, evaluationContext, methodExecuteResult, ret, customMethods);
            } catch (Exception e) {
                log.error("操作日志后置自定义函数解析失败", e);
            }
        }

        if (methodExecuteResult.getThrowable() != null) {
            throw methodExecuteResult.getThrowable();
        }
        return ret;
    }

    private void executeAfter(ProceedingJoinPoint joinPoint, Object target, Method method, OpLog opLog, String logContent,
                              OpLogEvaluationContext evaluationContext, MethodExecuteResult methodExecuteResult, Object ret, List<CustomMethodParams> customMethods) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (Objects.nonNull(requestAttributes)) {
            request = (HttpServletRequest) requestAttributes
                    .resolveReference(RequestAttributes.REFERENCE_REQUEST);
        } else {
            log.info("非web上下文，request为空");
        }
        String reqParams = covertMapStr(joinPoint);
        String respParams = JSONObject.toJSONString(ret);
        Map<String, String> mdcContext = new HashMap<>();
        try {
            mdcContext = MDC.getCopyOfContextMap();
        } catch (Exception e) {
            log.error("MDC上下文获取异常", e);
        }
        Map<String, String> finalMdcContext = mdcContext;
        HttpServletRequest finalRequest = request;
        Thread logThread = new Thread(() -> {
            try {
                MDC.setContextMap(finalMdcContext);
                String localLogContent = logContent;
                if (Objects.nonNull(evaluationContext)) {
                    // 代理方法执行后，添加返回值和错误信息和自定义变量
                    evaluationContext.addRet(ret, methodExecuteResult.getErrorMsg()).addVariables();
                }
                // 处理后置自定义函数
                if (CollectionUtils.isNotEmpty(customMethods)) {
                    localLogContent = processExecuteFunctionTemplate(customMethods, localLogContent, method, target, evaluationContext, false);
                }
                // 处理其他字段
                localLogContent = processExecuteParamTemplate(localLogContent, method, target, evaluationContext);
                // 日志处理
                OpLogRecord record = createRecord(opLog, localLogContent, finalRequest, reqParams, respParams);
                opLogRecordService.record(record);
            } catch (Exception e) {
                log.error("操作日志解析失败", e);
            }
        });
        logThread.setDaemon(true);
        logThread.start();
    }

    private OpLogRecord createRecord(OpLog opLog, String logContent, HttpServletRequest request, String reqParams, String respParams) {
        // 操作人
        SysLogOperator user = operatorGetService.getUser();
        return OpLogRecord.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userExtendInfo(user.getExtendInfo())
                .module(opLog.module())
                .category(opLog.category())
                .opTime(LocalDateTime.now())
                .content(logContent)
                .ip(getIpAddress(request))
                .deviceInfo(getUserAgent(request))
                .reqParams(reqParams)
                .respParams(respParams)
                .url(request.getRequestURI())
                .build();
    }

    public String covertMapStr(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        JSONObject rtnMap = new JSONObject();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            if (Objects.isNull(arg) || arg instanceof MultipartFile || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            rtnMap.put(paramNames[i], args[i]);
        }
        return rtnMap.toJSONString();
    }

    public String getUserAgent(HttpServletRequest request) {
        return Objects.nonNull(request) ? request.getHeader("User-Agent") : "";
    }

    public String getIpAddress(HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return "";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }


    private String processExecuteFunctionTemplate(List<CustomMethodParams> customMethods, String template, Method method, Object target, EvaluationContext evaluationContext, boolean executeBefore) {
        Map<String, String> cache = new HashMap<>();
        for (CustomMethodParams customMethod : customMethods) {
            if (functionService.beforeFunction(customMethod.getMethodName()) == executeBefore) {
                String result;
                String cacheKey = customMethod.getMethodName() + "_" + customMethod.getParamExpression();
                if (cache.containsKey(cacheKey)) {
                    result = cache.get(cacheKey);
                } else {
                    Object param = opLogValueParser.parseExpressionObj(customMethod.getParamExpression(), method, target, evaluationContext);
                    result = functionService.apply(customMethod.getMethodName(), param);
                    cache.put(cacheKey, result);
                }
                if (Objects.nonNull(result)) {
                    template = template.replaceFirst(Pattern.quote(customMethod.getMethodSourceExpression()), result);
                }
            }
        }
        return template;
    }

    private String processExecuteParamTemplate(String template, Method method, Object target, EvaluationContext evaluationContext) {
        List<CustomParams> params = OpLogRegular.getParams(template);
        for (CustomParams param : params) {
            String result = opLogValueParser.parseExpression(param.getSpelExpression(), method, target, evaluationContext);
            if (Objects.nonNull(result)) {
                template = template.replaceFirst(Pattern.quote(param.getSourceExpression()), result);
            }
        }
        return template;
    }
}
