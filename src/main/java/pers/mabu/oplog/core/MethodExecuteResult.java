package pers.mabu.oplog.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * 方法执行异常结果
 * </p>
 *
 * @author wangff
 * @since 2025/11/21
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MethodExecuteResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误
     */
    private Exception throwable;

    /**
     * 错误信息
     */
    private String errorMsg;
}
