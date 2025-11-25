package com.cool.store.oplog.core;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 操作日志上下文
 * </p>
 *
 * @author wangff
 * @since 2025/11/25
 */
public class OpLogThreadContext extends OpLogContext {

    public static void putEmptySpan() {
        variableMapStack.get().push(new HashMap<>());
    }

    /**
     * pop一个线程变量Map
     */
    public static void clear() {
        variableMapStack.get().pop();
    }

    public static Map<String, Object> getVariables() {
        return variableMapStack.get().peek();
    }
}
