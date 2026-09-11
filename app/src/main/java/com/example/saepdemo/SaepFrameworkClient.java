/*
 * Copyright (c) 2026 ByteDance Ltd. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.saepdemo;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

final class SaepFrameworkClient {
    private static final String ROBOTS_HELPER_STUB =
            "android.security.obric.robots.RobotsHelperStub";
    private static final String AUDIT_MANAGER_STUB =
            "android.security.obric.audit.SecurityAuditManagerStub";
    private static final String AGENT_MANAGER_STUB =
            "android.security.obric.agentmanager.AgentManagerStub";
    static final Uri POLICY_URI =
            Uri.parse("content://com.obric.agentrobots.provider/policy");

    private static final int MAX_ITEMS = 100;
    private static final int MAX_RENDERED_CHARS = 64 * 1024;
    private static final int MAX_FILTER_CHARS = 256;
    private static final int MAX_UUID_CHARS = 128;

    private final Context context;

    SaepFrameworkClient(Context context) {
        this.context = context.getApplicationContext();
    }

    CallResult queryProtocolEnabled() {
        try {
            Object stub = getStubInstance(ROBOTS_HELPER_STUB);
            Method method = stub.getClass().getDeclaredMethod("isRobotsEnabled", Context.class);
            method.setAccessible(true);
            Object value = method.invoke(stub, context);
            if (!(value instanceof Boolean)) {
                return CallResult.failure("isRobotsEnabled returned an unexpected value");
            }
            return CallResult.success("SAEP protocol enabled: " + value);
        } catch (ReflectiveOperationException | LinkageError e) {
            return reflectionFailure("isRobotsEnabled", e);
        }
    }

    String readStaticPolicyPreview() {
        try {
            String policy = readRawResource(R.raw.agent_saep_policy);
            SaepPolicy.validatePolicy(policy);
            return SaepPolicy.summarizePolicy(policy);
        } catch (IOException | IllegalArgumentException e) {
            return "Unable to read static policy: " + safeMessage(e);
        }
    }

    CallResult updateDynamicPolicy(SaepPolicy.Config config) {
        final String policy;
        try {
            policy = SaepPolicy.createDynamicPolicy(config);
        } catch (IllegalArgumentException e) {
            return CallResult.failure(safeMessage(e));
        }

        ContentValues values = new ContentValues(2);
        values.put("policy", policy);
        values.put("version", config.version);
        try {
            int rows = context.getContentResolver().update(POLICY_URI, values, null, null);
            if (rows <= 0) {
                return CallResult.failure(
                        "The provider accepted the call but updated no policy row. "
                                + "Confirm that the static policy was indexed after installation.");
            }
            return CallResult.success(String.format(Locale.US,
                    "Policy version %d updated successfully (%d row).%n%n%s",
                    config.version, rows, SaepPolicy.summarizePolicy(policy)));
        } catch (RuntimeException e) {
            return CallResult.failure("Policy update failed: " + safeMessage(e));
        }
    }

    CallResult queryLogs(long startTimeMs, long endTimeMs,
            String targetIntent, String targetActivity) {
        if (startTimeMs < 0 || endTimeMs < 0) {
            return CallResult.failure("Timestamps must be zero or positive");
        }
        if (startTimeMs > 0 && endTimeMs > 0 && startTimeMs > endTimeMs) {
            return CallResult.failure("Start time must not be later than end time");
        }
        if (!isValidFilter(targetIntent) || !isValidFilter(targetActivity)) {
            return CallResult.failure("Intent and Activity filters must not exceed "
                    + MAX_FILTER_CHARS + " characters");
        }

        try {
            Object stub = getStubInstance(AUDIT_MANAGER_STUB);
            Method method = stub.getClass().getDeclaredMethod("queryLogs",
                    long.class, long.class, String.class, String.class);
            method.setAccessible(true);
            Object value = method.invoke(stub,
                    startTimeMs, endTimeMs, targetIntent, targetActivity);
            if (!(value instanceof List)) {
                return CallResult.failure("queryLogs returned an unexpected value");
            }
            return CallResult.success(formatList("log", (List<?>) value));
        } catch (ReflectiveOperationException | LinkageError e) {
            return reflectionFailure("queryLogs", e);
        }
    }

    CallResult listRegisteredAgents() {
        try {
            Object stub = getStubInstance(AGENT_MANAGER_STUB);
            Method method = stub.getClass().getDeclaredMethod("listRegisteredAgents");
            method.setAccessible(true);
            Object value = method.invoke(stub);
            if (!(value instanceof List)) {
                return CallResult.failure("listRegisteredAgents returned an unexpected value");
            }
            return CallResult.success(formatList("registered Agent UUID", (List<?>) value));
        } catch (ReflectiveOperationException | LinkageError e) {
            return reflectionFailure("listRegisteredAgents", e);
        }
    }

    CallResult getAgentInfo(String agentUuid) {
        String normalizedUuid = agentUuid == null ? "" : agentUuid.trim();
        if (normalizedUuid.isEmpty() || normalizedUuid.length() > MAX_UUID_CHARS) {
            return CallResult.failure("Agent UUID must contain 1-" + MAX_UUID_CHARS
                    + " characters");
        }

        try {
            Object stub = getStubInstance(AGENT_MANAGER_STUB);
            Method method = stub.getClass().getDeclaredMethod("getAgentInfo", String.class);
            method.setAccessible(true);
            Object value = method.invoke(stub, normalizedUuid);
            if (value == null) {
                return CallResult.failure("No registered Agent matched this UUID");
            }
            return CallResult.success(String.valueOf(value));
        } catch (ReflectiveOperationException | LinkageError e) {
            return reflectionFailure("getAgentInfo", e);
        }
    }

    private static Object getStubInstance(String className) throws ReflectiveOperationException {
        try {
            Class<?> stubClass = Class.forName(className);
            Method getInstance = stubClass.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            Object instance = getInstance.invoke(null);
            if (instance == null) {
                throw new ReflectiveOperationException(className + " returned no instance");
            }
            return instance;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new ReflectiveOperationException(
                    "No supported SAEP framework Stub is available", e);
        }
    }

    private String readRawResource(int resourceId) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream stream = context.getResources().openRawResource(resourceId);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        return text.toString().trim();
    }

    private static boolean isValidFilter(String value) {
        return value == null || value.length() <= MAX_FILTER_CHARS;
    }

    private static String formatList(String itemLabel, List<?> values) {
        if (values.isEmpty()) {
            return "No " + itemLabel + " entries were returned.";
        }

        StringBuilder output = new StringBuilder();
        output.append("Total: ").append(values.size()).append('\n');
        int displayed = Math.min(values.size(), MAX_ITEMS);
        for (int i = 0; i < displayed; i++) {
            output.append('\n').append(i + 1).append(". ").append(values.get(i));
            if (output.length() >= MAX_RENDERED_CHARS) {
                output.setLength(MAX_RENDERED_CHARS);
                output.append("\n... output truncated");
                return output.toString();
            }
        }
        if (displayed < values.size()) {
            output.append("\n... showing first ").append(displayed).append(" entries");
        }
        return output.toString();
    }

    private static CallResult reflectionFailure(String operation, Throwable error) {
        Throwable cause = error;
        if (error instanceof InvocationTargetException
                && ((InvocationTargetException) error).getCause() != null) {
            cause = ((InvocationTargetException) error).getCause();
        }
        return CallResult.failure(operation + " failed: " + safeMessage(cause));
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = error.getClass().getSimpleName();
        }
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() <= 256 ? message : message.substring(0, 256) + "...";
    }

    static final class CallResult {
        final boolean success;
        final String message;

        private CallResult(boolean success, String message) {
            this.success = success;
            this.message = bound(message);
        }

        static CallResult success(String message) {
            return new CallResult(true, message);
        }

        static CallResult failure(String message) {
            return new CallResult(false, message);
        }

        private static String bound(String value) {
            if (value == null) {
                return "No details available";
            }
            return value.length() <= MAX_RENDERED_CHARS
                    ? value
                    : value.substring(0, MAX_RENDERED_CHARS) + "\n... output truncated";
        }
    }
}
