# SAEP Demo

Language: **[English](README.md)** | [简体中文](README.zh-CN.md)

Copyright (c) 2026 ByteDance Ltd. and/or its affiliates.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

## SAEP Overview

SAEP means **Screen Automation Execution Protocol**. In this demo, SAEP is treated as a platform integration surface for three app-side purposes:

- Declare an application protection policy that constrains screen automation.
- Inspect the identities of trusted Agents registered on the device.
- Verify the app's own operation history through local audit-log queries.

## Demo Scope And Security Constraints

The demo only:

- Reads the SAEP enablement switch.
- Declares and previews a packaged static policy.
- Builds and submits a dynamic policy for its own package, `com.example.saepdemo`.
- Queries caller-owned audit logs with optional filters.
- Lists trusted Agent UUIDs and queries one Agent detail by UUID.

## Supported Systems

APK installation compatibility and SAEP runtime capability are separate:

| Requirement | Current value |
| --- | --- |
| APK minimum SDK | `minSdk 23` |
| APK target SDK | `targetSdk 35` |
| APK compile SDK | `compileSdk 35` |
| Java source/target compatibility | Java 11 |
| Build inputs | Standard Gradle wrapper, JDK, and Android SDK |

At runtime, SAEP calls require a platform image exposing the obric Stub namespace and the policy provider:

- Required namespace: `android.security.obric.*`
- Required provider authority for dynamic policy: `com.obric.agentrobots.provider`

## Interface Reference

| UI capability | Interface used by the demo | Input parameters and validation | Return and failure behavior |
| --- | --- | --- | --- |
| SAEP Switch | `RobotsHelperStub.getInstance().isRobotsEnabled(Context)` through `android.security.obric.robots.RobotsHelperStub` | Uses the application `Context`. No user input. | Expects a `Boolean` and displays `SAEP protocol enabled: true` or `false`. Missing Stubs, linkage errors, reflection errors, or non-Boolean returns are displayed as failures. |
| Static Policy | Manifest metadata `com.obric.agentrobots.POLICY_JSON` pointing to `@raw/agent_saep_policy` | Packaged policy must pass the same policy validation used by dynamic policy: schema/package/version/timestamp/rule fields must match the implemented shape and size must be at most 10 KiB. | Displays a formatted preview with package and byte size. Resource, JSON, or validation errors display `Unable to read static policy: ...`. |
| Dynamic Policy | `ContentResolver.update(Uri, ContentValues, null, null)` at `content://com.obric.agentrobots.provider/policy` | `ContentValues` contains `policy` as the generated JSON string and `version` as the positive integer policy version. The UI accepts a positive `policy_version`; empty, non-integer, or `<= 0` values are rejected. The generated policy is bound to caller package `com.example.saepdemo`, uses current time in milliseconds for `updated_at`, and must be at most 10 KiB. | A returned row count `> 0` is success and displays the updated version, row count, and policy summary. A row count `<= 0` is failure. Provider `RuntimeException` is displayed as `Policy update failed: ...`. |
| Local Log Query | `SecurityAuditManagerStub.getInstance().queryLogs(long, long, String, String)` through `android.security.obric.audit.SecurityAuditManagerStub` | `startTimeMs` and `endTimeMs` are nonnegative millisecond timestamps. `0` means unbounded for that side of the range. If both are positive, start must not be later than end. `targetIntent` and `targetActivity` are optional trimmed strings; each may be empty and must not exceed 256 characters. | Expects a `List<?>`. Empty lists display no entries. Non-empty lists display total count and up to the first 100 entries, with rendered output capped at 64 KiB. Missing Stubs, reflection errors, linkage errors, or non-list returns are failures. |
| Trusted Agents: list | `AgentManagerStub.getInstance().listRegisteredAgents()` through `android.security.obric.agentmanager.AgentManagerStub` | No user input. | Expects a `List<?>` of registered Agent UUID entries. Empty lists display no entries. Non-empty lists display total count and up to the first 100 entries, capped at 64 KiB. Missing Stubs, reflection errors, linkage errors, or non-list returns are failures. |
| Trusted Agents: detail | `AgentManagerStub.getInstance().getAgentInfo(String)` through `android.security.obric.agentmanager.AgentManagerStub` | `agentUuid` is trimmed before the call. It must be nonempty and no longer than 128 characters in this demo. | A non-null returned object is displayed with `String.valueOf(value)`. `null` returns `No registered Agent matched this UUID`. Missing Stubs, reflection errors, linkage errors, or invalid UUID length are failures. |

All reflected Stub classes are resolved only from `android.security.obric.*`. Each Stub must expose a non-null `getInstance()` result.

## Policy Format

The implemented policy schema is `AGRP-Policy/1.0`. The static policy is packaged at `app/src/main/res/raw/agent_saep_policy.json`, and dynamic policy generation uses the same shape.

Top-level fields:

| Field | Type | Implemented rule |
| --- | --- | --- |
| `schema` | string | Must equal `AGRP-Policy/1.0`. |
| `policy_version` | integer | Must be positive. Static policy uses `1`; the dynamic screen defaults to `2`. |
| `package` | string | Must equal `com.example.saepdemo`; dynamic policy is always bound to this caller package. |
| `updated_at` | string | Must parse as a nonnegative millisecond timestamp. Dynamic policy uses `System.currentTimeMillis()`. |
| `default_policy` | object | Must contain `app` action rules. |
| `scope` | object | Must contain app rules, Main Activity rules, and supported Agent-intent rules. |

Action rule fields under `default_policy.app`, `scope.app`, and `scope.activities["com.example.saepdemo.MainActivity"].page_scope`:

| Field | Type | Meaning in this demo |
| --- | --- | --- |
| `global_disable` | boolean | Disable all Agent operations for that scope when `true`. |
| `screenshot_disable` | boolean | Disable screenshot operations for that scope when `true`. |
| `input_disable` | boolean | Disable input operations for that scope when `true`. |

Activity and Agent-intent fields:

| Field | Type | Implemented rule |
| --- | --- | --- |
| `scope.activities["com.example.saepdemo.MainActivity"].name` | string | Must not be empty. Current value is `SAEP Demo main screen`. |
| `scope.agent_intents.modify_content` | boolean | Disable the `modify_content` Agent intent when `true`. |
| `scope.agent_intents.post_content` | boolean | Disable the `post_content` Agent intent when `true`. |
| `scope.agent_intents.delete_content` | boolean | Disable the `delete_content` Agent intent when `true`. |
| `scope.agent_intents.account_incentive` | boolean | Disable the `account_incentive` Agent intent when `true`. |

The demo validates generated and packaged policy JSON before preview or update. Any policy larger than `10 * 1024` UTF-8 bytes is rejected.

## Build And Run

Use a standard JDK and Android SDK:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The APK can be installed on Android API 23 or later. On a device without SAEP framework support, the screens still render and policy preview still works, but switch, dynamic update, audit, and Agent queries report platform/provider unavailability.

## Runtime Notes

SAEP call results are rendered as either `Success` or `Unavailable or failed`. Error messages are shortened and line breaks are removed before display. Large list and policy outputs are capped to keep the demo responsive.

Dynamic policy updates use only the documented provider contract:

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

## SAEP Scope And Limitations

Supported operating system: ObricUI 2.2 or later.

The current SAEP protocol restricts and constrains four Agent intent categories: `modify_content`, `post_content`, `delete_content`, and `account_incentive`. Because business scenarios vary and large-language-model inference is inherently uncertain, SAEP does not guarantee that every operation will be identified and blocked successfully. Integrators should apply additional safeguards appropriate to their own use cases.

## Learn More

[Doubao Phone official website](https://o.doubao.com/developer)
