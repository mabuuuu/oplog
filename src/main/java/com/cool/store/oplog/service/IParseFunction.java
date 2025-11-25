package com.cool.store.oplog.service;

/**
 * <p>
 * 自定义函数
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public interface IParseFunction {
    default boolean executeBefore(){
        return false;
    }

    String functionName();

    String apply(Object value);
}
