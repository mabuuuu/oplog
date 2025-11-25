package com.cool.store.oplog.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志记录
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpLogRecord {
    /**
     * 操作人
     */
    private String userId;

    /**
     * 操作人姓名
     */
    private String userName;

    /**
     * 操作人扩展字段
     */
    private String userExtendInfo;

    /**
     * 模块
     */
    private String module;

    /**
     * 分类
     */
    private String category;

    /**
     * 操作时间
     */
    private LocalDateTime opTime;

    /**
     * 内容
     */
    private String content;

    /**
     * ip
     */
    private String ip;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 请求参数
     */
    private String reqParams;

    /**
     * 响应参数
     */
    private String respParams;

    /**
     * 请求路径
     */
    private String url;

    @Override
    public String toString() {
        return "{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userExtendInfo='" + userExtendInfo + '\'' +
                ", module='" + module + '\'' +
                ", category='" + category + '\'' +
                ", opTime=" + opTime +
                ", content='" + content + '\'' +
                ", ip='" + ip + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", reqParams='" + reqParams + '\'' +
                ", respParams='" + respParams + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
