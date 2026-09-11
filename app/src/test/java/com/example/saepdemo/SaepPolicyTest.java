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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public final class SaepPolicyTest {
    @Test
    public void buildPolicyUsesDocumentedShape() throws Exception {
        SaepPolicy.Config config = createConfig();
        config.version = 7;
        config.appScreenshotDisable = true;
        config.activityInputDisable = true;
        config.postContentDisable = true;

        String policy = SaepPolicy.buildPolicy(config, "1788566400000");
        JSONObject root = new JSONObject(policy);

        assertEquals("AGRP-Policy/1.0", root.getString("schema"));
        assertEquals(7, root.getInt("policy_version"));
        assertEquals("com.example.saepdemo", root.getString("package"));
        assertEquals("1788566400000", root.getString("updated_at"));
        assertTrue(root.getJSONObject("default_policy")
                .getJSONObject("app").getBoolean("screenshot_disable"));
        assertTrue(root.getJSONObject("scope").getJSONObject("activities")
                .getJSONObject(SaepPolicy.MAIN_ACTIVITY).getJSONObject("page_scope")
                .getBoolean("input_disable"));
        assertTrue(root.getJSONObject("scope").getJSONObject("agent_intents")
                .getBoolean("post_content"));
    }

    @Test
    public void buildPolicyIncludesOnlySupportedAgentIntents() throws Exception {
        JSONObject intents = new JSONObject(SaepPolicy.buildPolicy(
                createConfig(), "1788566400000"))
                .getJSONObject("scope")
                .getJSONObject("agent_intents");

        assertEquals(4, intents.length());
        assertTrue(intents.has("modify_content"));
        assertTrue(intents.has("post_content"));
        assertTrue(intents.has("delete_content"));
        assertTrue(intents.has("account_incentive"));
    }

    @Test
    public void validatePolicyRejectsMismatchedPackage() throws Exception {
        JSONObject root = new JSONObject(SaepPolicy.buildPolicy(
                createConfig(), "1788566400000"));
        root.put("package", "com.example.other");

        try {
            SaepPolicy.validatePolicy(root.toString());
            fail("Expected package validation to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("package"));
        }
    }

    @Test
    public void generatedPolicyFitsProviderLimit() {
        String policy = SaepPolicy.createDynamicPolicy(createConfig());

        assertTrue(policy.getBytes(StandardCharsets.UTF_8).length
                <= SaepPolicy.MAX_POLICY_BYTES);
        assertFalse(policy.contains("oem_groups"));
    }

    @Test
    public void buildPolicyRejectsNonTimestampUpdatedAt() {
        try {
            SaepPolicy.buildPolicy(createConfig(), "2026-09-05T00:00:00Z");
            fail("Expected updated_at validation to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("updated_at"));
        }
    }

    private static SaepPolicy.Config createConfig() {
        SaepPolicy.Config config = new SaepPolicy.Config();
        config.version = 2;
        return config;
    }
}
