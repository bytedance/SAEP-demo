# SAEP Demo

语言：[English](README.md) | **[简体中文](README.zh-CN.md)**

版权所有 (c) 2026 ByteDance Ltd. 和/或其关联公司。

本项目基于 Apache License, Version 2.0 授权。详见 [LICENSE](LICENSE)。

## SAEP 概览

SAEP 是 **Screen Automation Execution Protocol**。在本 Demo 中，SAEP 被视为平台侧集成接口，供应用完成三类验证：

- 声明应用保护策略，用于约束屏幕自动化能力。
- 查询设备上已注册可信 Agent 的身份信息。
- 通过本地审计日志查询验证应用自身操作记录。

## Demo 范围和安全约束

本 Demo 仅执行以下操作：

- 读取 SAEP 启用开关。
- 声明并预览打包在应用内的静态策略。
- 为自身包名 `com.example.saepdemo` 构造并提交动态策略。
- 使用可选过滤条件查询调用方自有审计日志。
- 列出可信 Agent UUID，并按 UUID 查询单个 Agent 详情。

## 支持系统

APK 安装兼容性和 SAEP 运行时能力是两件事：

| 要求 | 当前值 |
| --- | --- |
| APK 最低 SDK | `minSdk 23` |
| APK 目标 SDK | `targetSdk 35` |
| APK 编译 SDK | `compileSdk 35` |
| Java 源码/目标兼容性 | Java 11 |
| 构建输入 | 标准 Gradle wrapper、JDK 和 Android SDK |

运行时 SAEP 调用需要平台镜像暴露 obric Stub 命名空间和策略 provider：

- 必需命名空间：`android.security.obric.*`
- 动态策略所需 provider authority：`com.obric.agentrobots.provider`

## 接口参考

| UI 能力 | Demo 使用的接口 | 输入参数和校验 | 返回和失败行为 |
| --- | --- | --- | --- |
| SAEP 开关 | 通过 `android.security.obric.robots.RobotsHelperStub` 调用 `RobotsHelperStub.getInstance().isRobotsEnabled(Context)` | 使用应用 `Context`，无用户输入。 | 期望返回 `Boolean`，并显示 `SAEP protocol enabled: true` 或 `false`。Stub 缺失、linkage 错误、反射错误或返回值不是 Boolean 时显示失败。 |
| 静态策略 | Manifest 元数据 `com.obric.agentrobots.POLICY_JSON` 指向 `@raw/agent_saep_policy` | 打包策略必须通过与动态策略相同的校验：schema、package、version、timestamp、规则字段必须符合当前实现形状，大小不超过 10 KiB。 | 显示带 package 和字节大小的格式化预览。资源、JSON 或校验错误显示为 `Unable to read static policy: ...`。 |
| 动态策略 | 在 `content://com.obric.agentrobots.provider/policy` 上调用 `ContentResolver.update(Uri, ContentValues, null, null)` | `ContentValues` 包含 `policy` 字符串和 `version` 正整数。UI 接受正数 `policy_version`；空值、非整数或 `<= 0` 会被拒绝。生成的策略绑定到调用方包名 `com.example.saepdemo`，`updated_at` 使用当前毫秒时间，策略大小不超过 10 KiB。 | 返回行数 `> 0` 表示成功，并显示更新版本、行数和策略摘要。返回行数 `<= 0` 表示失败。Provider 抛出的 `RuntimeException` 显示为 `Policy update failed: ...`。 |
| 本地日志查询 | 通过 `android.security.obric.audit.SecurityAuditManagerStub` 调用 `SecurityAuditManagerStub.getInstance().queryLogs(long, long, String, String)` | `startTimeMs` 和 `endTimeMs` 是非负毫秒时间戳。`0` 表示对应边界不限制。若二者均为正数，开始时间不能晚于结束时间。`targetIntent` 和 `targetActivity` 是可选的 trim 后字符串，可为空，各自最长 256 个字符。 | 期望返回 `List<?>`。空列表显示无条目；非空列表显示总数和最多前 100 条，渲染输出上限 64 KiB。Stub 缺失、反射错误、linkage 错误或返回值不是 List 时显示失败。 |
| 可信 Agent：列表 | 通过 `android.security.obric.agentmanager.AgentManagerStub` 调用 `AgentManagerStub.getInstance().listRegisteredAgents()` | 无用户输入。 | 期望返回已注册 Agent UUID 条目的 `List<?>`。空列表显示无条目；非空列表显示总数和最多前 100 条，输出上限 64 KiB。Stub 缺失、反射错误、linkage 错误或返回值不是 List 时显示失败。 |
| 可信 Agent：详情 | 通过 `android.security.obric.agentmanager.AgentManagerStub` 调用 `AgentManagerStub.getInstance().getAgentInfo(String)` | `agentUuid` 在调用前会 trim。在本 Demo 中必须非空且不超过 128 个字符。 | 非 null 返回对象通过 `String.valueOf(value)` 显示。返回 `null` 时显示 `No registered Agent matched this UUID`。Stub 缺失、反射错误、linkage 错误或 UUID 长度非法时显示失败。 |

所有反射 Stub 类只从 `android.security.obric.*` 解析。每个 Stub 必须提供非 null 的 `getInstance()` 返回值。

## 策略格式

当前实现的策略 schema 为 `AGRP-Policy/1.0`。静态策略打包在 `app/src/main/res/raw/agent_saep_policy.json`，动态策略生成使用相同结构。

顶层字段：

| 字段 | 类型 | 当前实现规则 |
| --- | --- | --- |
| `schema` | string | 必须等于 `AGRP-Policy/1.0`。 |
| `policy_version` | integer | 必须为正数。静态策略使用 `1`；动态策略页面默认使用 `2`。 |
| `package` | string | 必须等于 `com.example.saepdemo`；动态策略始终绑定到此调用方包名。 |
| `updated_at` | string | 必须可解析为非负毫秒时间戳。动态策略使用 `System.currentTimeMillis()`。 |
| `default_policy` | object | 必须包含 `app` 动作规则。 |
| `scope` | object | 必须包含应用规则、Main Activity 规则和受支持的 Agent intent 规则。 |

`default_policy.app`、`scope.app` 和 `scope.activities["com.example.saepdemo.MainActivity"].page_scope` 下的动作规则字段：

| 字段 | 类型 | 在本 Demo 中的含义 |
| --- | --- | --- |
| `global_disable` | boolean | 为 `true` 时禁用该范围内所有 Agent 操作。 |
| `screenshot_disable` | boolean | 为 `true` 时禁用该范围内截图操作。 |
| `input_disable` | boolean | 为 `true` 时禁用该范围内输入操作。 |

Activity 和 Agent intent 字段：

| 字段 | 类型 | 当前实现规则 |
| --- | --- | --- |
| `scope.activities["com.example.saepdemo.MainActivity"].name` | string | 不能为空。当前值为 `SAEP Demo main screen`。 |
| `scope.agent_intents.modify_content` | boolean | 为 `true` 时禁用 `modify_content` Agent intent。 |
| `scope.agent_intents.post_content` | boolean | 为 `true` 时禁用 `post_content` Agent intent。 |
| `scope.agent_intents.delete_content` | boolean | 为 `true` 时禁用 `delete_content` Agent intent。 |
| `scope.agent_intents.account_incentive` | boolean | 为 `true` 时禁用 `account_incentive` Agent intent。 |

Demo 会在预览或更新前校验生成和打包的策略 JSON。超过 `10 * 1024` UTF-8 字节的策略会被拒绝。

## 构建和运行

使用标准 JDK 和 Android SDK：

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

APK 可安装在 Android API 23 或更高版本。若设备没有 SAEP 框架支持，页面仍可渲染且静态/动态策略预览仍可使用，但开关、动态更新、审计和 Agent 查询会报告平台或 provider 不可用。

## 运行时说明

SAEP 调用结果会显示为 `Success` 或 `Unavailable or failed`。错误消息在展示前会移除换行并截短。大型列表和策略输出会被限制长度，以保持 Demo 响应性。

动态策略更新仅使用已文档化的 provider 契约：

```java
ContentValues values = new ContentValues(2);
values.put("policy", policy);
values.put("version", config.version);
context.getContentResolver().update(
        Uri.parse("content://com.obric.agentrobots.provider/policy"),
        values,
        null,
        null);
```

## SAEP 适用范围与限制

可使用的操作系统：ObricUI 2.2 版本及以上。

当前 SAEP 协议仅针对 `modify_content`、`post_content`、`delete_content` 和 `account_incentive` 四类 Agent 意图进行限制和约束。由于不同业务场景存在差异，且大模型推理具有不确定性，SAEP 不保证每一次操作都能被准确识别并成功拦截；接入方应结合具体业务配置必要的补充安全措施。

## 了解更多

[豆包手机官网](https://o.doubao.com/developer)
