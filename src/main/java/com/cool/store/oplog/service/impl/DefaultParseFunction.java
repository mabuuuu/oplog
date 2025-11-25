package com.cool.store.oplog.service.impl;

import com.cool.store.oplog.service.IParseFunction;

/**
 * <p>
 * 默认自定义函数
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class DefaultParseFunction implements IParseFunction {
    @Override
    public String functionName() {
        return "defaultFunction";
    }

    @Override
    public String apply(Object value) {
        return "";
    }
}
