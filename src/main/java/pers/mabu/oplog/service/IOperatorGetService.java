package pers.mabu.oplog.service;

import pers.mabu.oplog.core.SysLogOperator;

/**
 * <p>
 * 操作人逻辑
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public interface IOperatorGetService {

    SysLogOperator getUser();
}
