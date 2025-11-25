package com.cool.store.oplog.core;

import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 操作日志SpEL表达式解析器
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
public class OpLogExpressionEvaluator extends CachedExpressionEvaluator {
    // 缓存方法、表达式和 SpEL 的 Expression 的对应关系
    private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>(64);

    private final static ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public Object parseExpression(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.expressionCache, methodKey, conditionExpression).getValue(evalContext);
    }

    public OpLogEvaluationContext createEvaluationContext(Method method, Object[] args, Object target, Object ret, String errorMsg) {
        return new OpLogEvaluationContext(target, method, args, parameterNameDiscoverer, ret, errorMsg);
    }
}
