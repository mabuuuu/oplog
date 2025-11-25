package com.cool.store.oplog.aspect;

import com.cool.store.oplog.annotation.OpLog;
import com.cool.store.oplog.core.*;
import com.cool.store.oplog.service.IFunctionService;
import com.cool.store.oplog.service.IOpLogRecordService;
import com.cool.store.oplog.service.IOperatorGetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@ConditionalOnProperty(name = "oplog.enable", havingValue = "true")
public class OpLogAspect {
    private final OpLogValueParser opLogValueParser;
    private final IFunctionService functionService;
    private final IOperatorGetService operatorGetService;
    private final IOpLogRecordService opLogRecordService;

    @Pointcut("@annotation(com.cool.store.oplog.annotation.OpLog)")
    public void pointcut() {
    }

    @Around(value = "@annotation(opLog)")
    public Object opLogAdvice(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        log.info("进入操作日志增强");
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
            OpLogThreadContext.putEmptySpan();
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
                log.error("操作日志前置自定义函数解析失败");
            }
        }

        try {
            ret = joinPoint.proceed();
        } catch (Exception e) {
            methodExecuteResult = new MethodExecuteResult(false, e, e.getMessage());
        }

        try {
            if (methodExecuteResult.isSuccess() && condition) {
                if (Objects.nonNull(evaluationContext)) {
                    // 代理方法执行后，添加返回值和错误信息和自定义变量
                    evaluationContext.addRet(ret, methodExecuteResult.getErrorMsg()).addVariables();
                }
                // 处理后置自定义函数
                if (CollectionUtils.isNotEmpty(customMethods)) {
                    logContent = processExecuteFunctionTemplate(customMethods, logContent, method, target, evaluationContext, false);
                }
                // 处理其他字段
                logContent = processExecuteParamTemplate(logContent, method, target, evaluationContext);
                // 日志处理
                OpLogRecord record = createRecord(joinPoint, opLog, logContent, ret);
                opLogRecordService.record(record);
            }
        } catch (Exception e) {
            log.error("操作日志解析失败", e);
        } finally {
            OpLogThreadContext.clear();
        }

        if (methodExecuteResult.getThrowable() != null) {
            throw methodExecuteResult.getThrowable();
        }
        return ret;
    }

    private OpLogRecord createRecord(JoinPoint joinPoint, OpLog opLog, String logContent, Object ret) throws JsonProcessingException {
        // 获取RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) requestAttributes
                .resolveReference(RequestAttributes.REFERENCE_REQUEST);
        // 操作人
        SysLogOperator user = operatorGetService.getUser();
        ObjectMapper mapper = new ObjectMapper();
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
                .reqParams(mapper.writeValueAsString(covertMap(joinPoint)))
                .respParams(mapper.writeValueAsString(ret))
                .url(request.getRequestURI())
                .build();
    }

    public Map<String, Object> covertMap(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        Map<String, Object> rtnMap = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            if (Objects.isNull(arg) || arg instanceof MultipartFile || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            rtnMap.put(paramNames[i], args[i]);
        }
        return rtnMap;
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
