package com.cool.store.oplog.service.impl;

import com.cool.store.oplog.core.SysLogOperator;
import com.cool.store.oplog.service.IOperatorGetService;

/**
 * <p>
 * 默认操作人
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class DefaultOperatorGetServiceImpl implements IOperatorGetService {
    @Override
    public SysLogOperator getUser() {
        return new SysLogOperator("defaultUserId", "defaultUserName", "");
    }
}
