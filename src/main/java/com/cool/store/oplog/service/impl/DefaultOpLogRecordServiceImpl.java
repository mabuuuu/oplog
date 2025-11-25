package com.cool.store.oplog.service.impl;

import com.cool.store.oplog.core.OpLogRecord;
import com.cool.store.oplog.service.IOpLogRecordService;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 默认操作日志记录
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Slf4j
public class DefaultOpLogRecordServiceImpl implements IOpLogRecordService {
    @Override
    public void record(OpLogRecord opLogRecord) {
        log.info("操作日志：{}", opLogRecord.toString());
    }
}
