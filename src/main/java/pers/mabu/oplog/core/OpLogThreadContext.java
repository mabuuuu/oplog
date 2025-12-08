package pers.mabu.oplog.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * <p>
 * 操作日志上下文
 * </p>
 *
 * @author wangff
 * @since 2025/11/25
 */
@Slf4j
public class OpLogThreadContext extends OpLogContext {

    public static Map<String, Object> getVariables() {
        return variableMapStack.get();
    }
}
