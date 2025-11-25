package com.cool.store.oplog.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 自定义函数参数
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomMethodParams {
    /**
     * 方法原表达式，例如{getUser({#userId})}
     */
    private String methodSourceExpression;

    /**
     * 方法名，例如getUser
     */
    private String methodName;
    
    /**
     * 入参SpEL表达式，例如#userId
     */
    private String paramExpression;
}
