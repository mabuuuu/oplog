package pers.mabu.oplog.core;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 操作日志上下文，基于栈式结构支持嵌套 {@code @OpLog} 调用的变量隔离。
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
public class OpLogContext {
    private static final TransmittableThreadLocal<Deque<Map<String, Object>>> variableMapStack = new TransmittableThreadLocal<>();

    private static Deque<Map<String, Object>> getStack() {
        return variableMapStack.get();
    }

    private static Deque<Map<String, Object>> getOrCreateStack() {
        Deque<Map<String, Object>> stack = variableMapStack.get();
        if (Objects.isNull(stack)) {
            stack = new ArrayDeque<>();
            variableMapStack.set(stack);
        }
        return stack;
    }

    /**
     * 初始化当前层级的变量上下文（在 execute 入口调用）
     */
    public static void initLevel() {
        getOrCreateStack().push(new HashMap<>());
    }

    public static void putVariable(String key, Object value) {
        Deque<Map<String, Object>> stack = getOrCreateStack();
        Map<String, Object> map = stack.peek();
        if (Objects.isNull(map)) {
            map = new HashMap<>();
            stack.push(map);
        }
        map.put(key, value);
    }

    public static Map<String, Object> getVariables() {
        Deque<Map<String, Object>> stack = getStack();
        return Objects.isNull(stack) ? null : stack.peek();
    }

    /**
     * 弹出当前层级的变量，恢复上一层。栈空时移除 ThreadLocal 防止内存泄漏。
     */
    public static void clear() {
        Deque<Map<String, Object>> stack = getStack();
        if (Objects.isNull(stack)) {
            return;
        }
        stack.pollFirst();
        if (stack.isEmpty()) {
            variableMapStack.remove();
        }
    }
}
