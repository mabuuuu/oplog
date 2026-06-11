# @OpLog 注解使用说明

## 注解参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `success` | String | 是 | 日志模板，支持 SpEL 表达式和自定义函数 |
| `module` | String | 否 | 模块名，默认 `""` |
| `category` | String | 否 | 分类名，默认 `""` |
| `condition` | String | 否 | 记录条件 SpEL 表达式，为 true 才记录，默认 `""` 表示始终记录 |

---

## 模板语法

### 1. 基本表达式 `{{#param}}`

引用方法参数，用 `#` 前缀加参数名：

```java
@OpLog(success = "新建用户{{#userName}}，角色{{#roleName}}")
public void createUser(String userName, String roleName) {
    // → 新建用户张三，角色管理员
}
```

### 2. 嵌套属性 `.` 导航

```java
@OpLog(success = "修改{{#user.name}}的城市为{{#user.address.city}}")
public void updateCity(User user) {
    // → 修改张三的城市为北京
}
```

### 3. 返回值 `{{#_ret}}`

```java
@OpLog(success = "下单成功，订单号:{{#_ret.orderNo}}")
public Order createOrder(OrderRequest req) {
    return new Order("ORD001");
    // → 下单成功，订单号:ORD001
}
```

### 4. 自定义函数 `{funcName{#param}}`

实现 `IParseFunction` 接口并注册为 Spring Bean，对参数做二次加工：

```java
@Component
public class GetDeptName implements IParseFunction {

    @Override
    public String functionName() { return "getDeptName"; }

    @Override
    public String apply(Object value) {
        return deptService.getName((Long) value);
    }
}
```

```java
@OpLog(success = "将用户从[{getDeptName{#deptId}}]部门调入[{{#newDept}}]")
public void transfer(Long deptId, String newDept) {
    // → 将用户从[研发部]部门调入[产品部]
}
```

### 5. 前置函数（方法执行前取值）

`executeBefore()` 返回 `true` 的函数在**方法执行前**调用，适合获取旧值：

```java
@Component
public class GetOldName implements IParseFunction {

    @Override
    public boolean executeBefore() { return true; }

    @Override
    public String functionName() { return "getOldName"; }

    @Override
    public String apply(Object value) {
        return userMapper.getName((Long) value);
    }
}
```

```java
@OpLog(success = "用户名从[{getOldName{#userId}}]改为[{{#newName}}]")
public void rename(Long userId, String newName) {
    // getOldName 在 rename 执行前调用，拿到旧名称
    // → 用户名从[张三]改为[李四]
}
```

### 6. 上下文变量

方法体内通过 `OpLogContext.putVariable()` 设置变量，模板中用 `{{#varName}}` 引用：

```java
@OpLog(success = "删除{{#count}}条记录")
public void batchDelete(List<Long> ids) {
    int count = mapper.deleteBatch(ids);
    OpLogContext.putVariable("count", count);
    // → 删除5条记录
}
```

---

## 条件记录 `condition`

```java
// 金额 > 1000 才记录
@OpLog(success = "大额交易:{{#amount}}", condition = "#amount > 1000")

// 操作结果不为空才记录
@OpLog(success = "操作结果:{{#_ret}}", condition = "#_ret != null")

// 特定用户才记录
@OpLog(success = "VIP操作:{{#action}}", condition = "#user.vip == true")
```

---

## 完整示例

```java
@OpLog(
    success = "订单{{#orderId}}金额从[{getOldAmount{#orderId}}]变更为[{{#newAmount}}]，操作结果:{{#_ret}}",
    module = "订单管理",
    category = "修改金额",
    condition = "#newAmount > 0"
)
public boolean updateAmount(Long orderId, BigDecimal newAmount) {
    boolean result = orderMapper.updateAmount(orderId, newAmount);
    OpLogContext.putVariable("operatorIp", getCurrentIp());
    return result;
}
```

执行后日志内容：`订单1001金额从[199.00]变更为[299.00]，操作结果:true`
