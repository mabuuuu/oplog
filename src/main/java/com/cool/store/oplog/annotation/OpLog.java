package com.cool.store.oplog.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OpLog {
    // 调用成功时模板
    String success();
    // 模块
    String module() default "";
    // 分类
    String category() default "";
    // 记录日志的条件
    String condition() default "";
}
