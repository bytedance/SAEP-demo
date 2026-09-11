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

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class SaepStaticPolicyResourceTest {
    @Test
    public void staticPolicyMatchesManifestPackageAndSchema() throws Exception {
        String policy = readResource("/raw/agent_saep_policy.json");
        SaepPolicy.validatePolicy(policy);

        JSONObject root = new JSONObject(policy);
        assertEquals("AGRP-Policy/1.0", root.getString("schema"));
        assertEquals(SaepPolicy.PACKAGE_NAME, root.getString("package"));
        assertEquals(SaepPolicy.STATIC_POLICY_VERSION, root.getInt("policy_version"));
        assertFalse(root.has("oem_groups"));
    }

    private String readResource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
