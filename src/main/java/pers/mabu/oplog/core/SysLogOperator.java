package pers.mabu.oplog.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 系统日志操作人
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysLogOperator {
    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 操作人扩展字段
     */
    private String extendInfo;
}
