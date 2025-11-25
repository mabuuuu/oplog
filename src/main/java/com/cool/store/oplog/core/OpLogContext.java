package com.cool.store.oplog.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * <p>
 * 操作日志上下文
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
public class OpLogContext {
    protected static final InheritableThreadLocal<Stack<Map<String, Object>>> variableMapStack;

    static {
        variableMapStack = new InheritableThreadLocal<>();
        variableMapStack.set(new Stack<>());
    }

    public static void putVariable(String key, Object value) {
        variableMapStack.get().peek().put(key, value);
    }

    public static Object getVariable(String key) {
        return variableMapStack.get().peek().get(key);
    }
}
