package pers.mabu.oplog.core;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>
 * 操作日志表达式上下文
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class OpLogEvaluationContext extends MethodBasedEvaluationContext {

    public OpLogEvaluationContext(Object rootObject, Method method, Object[] arguments,
                                      ParameterNameDiscoverer parameterNameDiscoverer, Object ret, String errorMsg) {
        //把方法的参数都放到 SpEL 解析的 RootObject 中
        super(rootObject, method, arguments, parameterNameDiscoverer);
        //把 LogRecordContext 中的变量都放到 RootObject 中
        Map<String, Object> variables = OpLogThreadContext.getVariables();
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                setVariable(entry.getKey(), entry.getValue());
            }
        }
        //把方法的返回值和 ErrorMsg 都放到 RootObject 中
        setVariable("_ret", ret);
        setVariable("_errorMsg", errorMsg);
    }

    public OpLogEvaluationContext addRet(Object ret, String errorMsg) {
        setVariable("_ret", ret);
        setVariable("_errorMsg", errorMsg);
        return this;
    }

    public OpLogEvaluationContext addVariables() {
        Map<String, Object> variables = OpLogThreadContext.getVariables();
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                setVariable(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }
}
