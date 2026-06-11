package pers.mabu.oplog.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.task.TaskExecutor;
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

import java.io.InputStream;
import java.io.OutputStream;
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
    private final TaskExecutor executor;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

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
            try {
                evaluationContext = opLogValueParser.createEvaluationContext(method, args, target, ret, methodExecuteResult.getErrorMsg());
                if (StringUtils.isNotBlank(opLog.condition())) {
                    condition = opLogValueParser.parseExpressionCondition(opLog.condition(), method, target, evaluationContext);
                }
            } catch (Exception e) {
                log.error("操作日志条件表达式解析失败", e);
            }
            if (condition) {
                try {
                    customMethods = OpLogRegular.getMethod(opLog.success());
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
        } finally {
            OpLogContext.clear();
        }
    }

    private void executeAfter(ProceedingJoinPoint joinPoint, Object target, Method method, OpLog opLog, String logContent,
                              OpLogEvaluationContext evaluationContext, MethodExecuteResult methodExecuteResult, Object ret, List<CustomMethodParams> customMethods) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Object request = null;
        if (Objects.nonNull(requestAttributes)) {
            request = requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        } else {
            log.info("非web上下文，request为空");
        }
        String reqParams = covertMapStr(joinPoint);
        String respParams = toJsonString(ret);
        Map<String, String> mdcContext = new HashMap<>();
        try {
            mdcContext = MDC.getCopyOfContextMap();
        } catch (Exception e) {
            log.error("MDC上下文获取异常", e);
        }
        Map<String, String> finalMdcContext = mdcContext;
        Object finalRequest = request;
        executor.execute(() -> {
            try {
                if (finalMdcContext != null) {
                    MDC.setContextMap(finalMdcContext);
                }
                String localLogContent = logContent;
                if (Objects.nonNull(evaluationContext)) {
                    evaluationContext.addRet(ret, methodExecuteResult.getErrorMsg()).addVariables();
                }
                if (CollectionUtils.isNotEmpty(customMethods)) {
                    localLogContent = processExecuteFunctionTemplate(customMethods, localLogContent, method, target, evaluationContext, false);
                }
                localLogContent = processExecuteParamTemplate(localLogContent, method, target, evaluationContext);
                OpLogRecord record = createRecord(opLog, localLogContent, finalRequest, reqParams, respParams);
                opLogRecordService.record(record);
            } catch (Exception e) {
                log.error("操作日志解析失败", e);
            } finally {
                OpLogContext.clear();
            }
        });
    }

    private OpLogRecord createRecord(OpLog opLog, String logContent, Object request, String reqParams, String respParams) {
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
                .url(getRequestURI(request))
                .build();
    }

    public String covertMapStr(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        Map<String, Object> rtnMap = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            if (Objects.isNull(arg)
                    || arg instanceof MultipartFile
                    || arg instanceof InputStream
                    || arg instanceof OutputStream
                    || isServletRequest(arg)
                    || isServletResponse(arg)) {
                continue;
            }
            rtnMap.put(paramNames[i], args[i]);
        }
        return toJsonString(rtnMap);
    }

    public String getUserAgent(Object request) {
        return getHeader(request, "User-Agent");
    }

    public String getIpAddress(Object request) {
        if (Objects.isNull(request)) {
            return "";
        }
        String ip = getHeader(request, "x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getHeader(request, "Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getHeader(request, "WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = getRemoteAddr(request);
        }
        return ip;
    }

    // ---- servlet abstraction helpers (supports both javax.servlet and jakarta.servlet) ----

    private static boolean isServletRequest(Object obj) {
        if (obj == null) return false;
        Class<?> clazz = obj.getClass();
        do {
            if ("javax.servlet.http.HttpServletRequest".equals(clazz.getName())
                    || "jakarta.servlet.http.HttpServletRequest".equals(clazz.getName())) {
                return true;
            }
            for (Class<?> iface : clazz.getInterfaces()) {
                String name = iface.getName();
                if ("javax.servlet.http.HttpServletRequest".equals(name)
                        || "jakarta.servlet.http.HttpServletRequest".equals(name)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return false;
    }

    private static boolean isServletResponse(Object obj) {
        if (obj == null) return false;
        Class<?> clazz = obj.getClass();
        do {
            if ("javax.servlet.http.HttpServletResponse".equals(clazz.getName())
                    || "jakarta.servlet.http.HttpServletResponse".equals(clazz.getName())) {
                return true;
            }
            for (Class<?> iface : clazz.getInterfaces()) {
                String name = iface.getName();
                if ("javax.servlet.http.HttpServletResponse".equals(name)
                        || "jakarta.servlet.http.HttpServletResponse".equals(name)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return false;
    }

    private static String getHeader(Object request, String name) {
        try {
            return (String) request.getClass().getMethod("getHeader", String.class).invoke(request, name);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getRemoteAddr(Object request) {
        try {
            return (String) request.getClass().getMethod("getRemoteAddr").invoke(request);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getRequestURI(Object request) {
        if (Objects.isNull(request)) {
            return "";
        }
        try {
            return (String) request.getClass().getMethod("getRequestURI").invoke(request);
        } catch (Exception e) {
            return "";
        }
    }

    private String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "";
        }
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
