package pers.mabu.oplog.core;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 操作日志上下文
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
public class OpLogContext {
    protected static final TransmittableThreadLocal<Map<String, Object>> variableMapStack = new TransmittableThreadLocal<>();

    public static void putVariable(String key, Object value) {
        Map<String, Object> map = variableMapStack.get();
        if (Objects.isNull(map)) {
            map = new HashMap<>();
            variableMapStack.set(map);
        }
        map.put(key, value);
    }

    public static Object getVariable(String key) {
        Map<String, Object> map = variableMapStack.get();
        if (Objects.isNull(map)) {
            return null;
        }
        return map.get(key);
    }

    /**
     * 清理当前线程的变量，防止线程池场景下的内存泄漏
     */
    public static void clear() {
        variableMapStack.remove();
    }

    public static Map<String, Object> getVariables() {
        return variableMapStack.get();
    }
}
