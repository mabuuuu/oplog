package pers.mabu.oplog.core;

import lombok.Synchronized;

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
    protected static final InheritableThreadLocal<Map<String, Object>> variableMapStack = new InheritableThreadLocal<>();


    @Synchronized
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
}
