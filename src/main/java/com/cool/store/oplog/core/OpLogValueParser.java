package com.cool.store.oplog.core;

import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * <p>
 * 操作日志分析器
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class OpLogValueParser {
    private OpLogExpressionEvaluator expressionEvaluator;

    public OpLogValueParser(OpLogExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    public String parseExpression(String conditionExpression, Method method, Object target, EvaluationContext evaluationContext) {
        Object obj = expressionEvaluator.parseExpression(conditionExpression, new AnnotatedElementKey(method, target.getClass()), evaluationContext);
        return Objects.nonNull(obj) ? obj.toString() : "";
    }

    public Object parseExpressionObj(String conditionExpression, Method method, Object target, EvaluationContext evaluationContext) {
        return expressionEvaluator.parseExpression(conditionExpression, new AnnotatedElementKey(method, target.getClass()), evaluationContext);
    }

    public Boolean parseExpressionCondition(String conditionExpression, Method method, Object target, EvaluationContext evaluationContext) {
        return (Boolean) expressionEvaluator.parseExpression(conditionExpression, new AnnotatedElementKey(method, target.getClass()), evaluationContext);
    }

    public OpLogEvaluationContext createEvaluationContext(Method method, Object[] args, Object target, Object ret, String errorMsg) {
        return expressionEvaluator.createEvaluationContext(method, args, target, ret, errorMsg);
    }
}
