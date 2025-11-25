package com.cool.store.oplog.service;

/**
 * <p>
 * 自定义函数
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public interface IFunctionService {

    String apply(String functionName, Object value);

    /**
     * 前置方法
     */
    boolean beforeFunction(String functionName);
}
