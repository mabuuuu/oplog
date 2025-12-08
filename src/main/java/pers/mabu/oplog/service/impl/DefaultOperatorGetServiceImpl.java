package pers.mabu.oplog.service.impl;

import pers.mabu.oplog.core.SysLogOperator;
import pers.mabu.oplog.service.IOperatorGetService;

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
