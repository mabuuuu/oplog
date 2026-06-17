##
## ProGuard 混淆规则 — oplog Spring Boot Starter
##
## 设计意图：
##   1. 保留对外 API（注解、SPI接口、用户工具类）
##   2. 保留 Spring 自动配置所需的类名和注解
##   3. 混淆所有内部实现的方法名、字段名
##   4. 不做 shrink/optimize，避免误删 Spring 代理和反射依赖
##

# ---- 全局 ----
-dontshrink
-dontoptimize
-dontpreverify

# 保留注解、泛型签名、异常表、行号（便于错误定位）
-keepattributes *Annotation*,Signature,InnerClasses,Exceptions,LineNumberTable,SourceFile

# 保留所有 Spring/AspectJ 相关的运行时注解
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations

# 保留类初始化方法
-keepclassmembers class * {
    <init>(...);
}

# 抑制来自第三方依赖的警告（Spring Boot, AspectJ, Lombok 等）
-dontwarn org.springframework.**
-dontwarn org.aspectj.**
-dontwarn lombok.**
-dontwarn com.fasterxml.**
-dontwarn com.alibaba.**
-dontwarn org.slf4j.**
-dontwarn org.apache.**
-dontwarn javax.**
-dontwarn jakarta.**
-dontwarn reactor.**

# ---- Level 1: 对外 API（完全保留，用户直接使用）----

# OpLog 注解
-keep @interface pers.mabu.oplog.annotation.OpLog { *; }

# SPI 扩展接口
-keep interface pers.mabu.oplog.service.IOperatorGetService { *; }
-keep interface pers.mabu.oplog.service.IFunctionService { *; }
-keep interface pers.mabu.oplog.service.IOpLogRecordService { *; }
-keep interface pers.mabu.oplog.service.IParseFunction { *; }

# 用户工具类
-keep public class pers.mabu.oplog.core.OpLogContext {
    public <methods>;
}
-keep public class pers.mabu.oplog.core.SysLogOperator {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.core.OpLogRecord {
    public <methods>;
    public <init>(...);
}
# 保留 Builder 内部类
-keep public class pers.mabu.oplog.core.OpLogRecord$* {
    public <methods>;
    public <init>(...);
}

# ---- Level 2: Spring 配置 / Bean 类型（保留类名，混淆成员）----

# Spring 自动配置入口（spring.factories 引用）
-keep public class pers.mabu.oplog.config.OpLogProxyAutoConfiguration {
    public <methods>;
    public <init>(...);
}

# AOP 切面（ConditionalOnMissingBean 引用）
-keep public class pers.mabu.oplog.aspect.OpLogAspect {
    public <methods>;
    public <init>(...);
}

# Bean 类型（Spring 依赖注入的类型引用）
-keep public class pers.mabu.oplog.core.OpLogValueParser {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.core.OpLogExpressionEvaluator {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.core.OpLogEvaluationContext {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.core.MdcTaskExecutor {
    public <methods>;
    public <init>(...);
}

# SPI 工厂（自动配置方法参数类型）
-keep public class pers.mabu.oplog.service.ParseFunctionFactory {
    public <methods>;
    public <init>(...);
}

# ---- Level 3: 默认实现（保留公共方法，用户可能查阅和继承）----

-keep public class pers.mabu.oplog.service.impl.DefaultFunctionServiceImpl {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.service.impl.DefaultOpLogRecordServiceImpl {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.service.impl.DefaultOperatorGetServiceImpl {
    public <methods>;
    public <init>(...);
}
-keep public class pers.mabu.oplog.service.impl.DefaultParseFunction {
    public <methods>;
    public <init>(...);
}

# ---- Level 4: 以下内部工具类全部混淆（无保留规则）----
#   pers.mabu.oplog.core.MethodExecuteResult
#   pers.mabu.oplog.core.CustomMethodParams
#   pers.mabu.oplog.core.CustomParams
#   pers.mabu.oplog.core.OpLogRegular
