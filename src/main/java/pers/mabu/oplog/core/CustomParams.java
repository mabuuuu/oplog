package pers.mabu.oplog.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 参数
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomParams {
    /**
     * 原表达式
     */
    private String sourceExpression;
    
    /**
     * SpEL表达式
     */
    private String spelExpression;
}
