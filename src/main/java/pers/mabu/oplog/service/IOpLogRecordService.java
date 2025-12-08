package pers.mabu.oplog.service;

import pers.mabu.oplog.core.OpLogRecord;

/**
 * <p>
 * 操作日志记录
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public interface IOpLogRecordService {

    /**
     * 保存log
     */
    void record(OpLogRecord opLogRecord);
}
