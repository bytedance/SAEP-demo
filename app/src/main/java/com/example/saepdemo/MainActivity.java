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

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SaepFrameworkClient client;
    private LinearLayout content;
    private TextView output;
    private String currentScreen = "home";
    private volatile int screenGeneration;
    private boolean callInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new SaepFrameworkClient(this);
        showHome();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (!"home".equals(currentScreen)) {
            showHome();
            return;
        }
        super.onBackPressed();
    }

    private void showHome() {
        currentScreen = "home";
        beginScreen("SAEP Demo", "Standalone verification app for the SAEP framework surface.");
        addNavButton("SAEP Switch", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSwitch();
            }
        });
        addNavButton("Static Policy", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStaticPolicy();
            }
        });
        addNavButton("Dynamic Policy", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDynamicPolicy();
            }
        });
        addNavButton("Local Log Query", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocalLogs();
            }
        });
        addNavButton("Trusted Agents", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTrustedAgents();
            }
        });
        setOutput("Choose a capability. Calls use reflection only and report missing platform APIs explicitly.");
    }

    private void showSwitch() {
        currentScreen = "switch";
        beginScreen("SAEP Switch", "Query the framework-level SAEP enablement state.");
        addBackButton();
        Button read = addActionButton("Read Switch");
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runSaepCall("Reading switch", new SaepTask() {
                    @Override
                    public SaepFrameworkClient.CallResult run() {
                        return client.queryProtocolEnabled();
                    }
                });
            }
        });
        setOutput(getString(R.string.saep_status_unknown));
    }

    private void showStaticPolicy() {
        currentScreen = "static";
        beginScreen("Static Policy", "Manifest metadata points at the packaged policy below.");
        addBackButton();
        Button preview = addActionButton("Preview Static Policy");
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setOutput(client.readStaticPolicyPreview());
            }
        });
        setOutput(client.readStaticPolicyPreview());
    }

    private void showDynamicPolicy() {
        currentScreen = "dynamic";
        beginScreen("Dynamic Policy", "Update the documented SAEP policy provider contract.");
        addBackButton();

        final EditText versionInput = addEditText("Policy version", "2", true);
        addSectionLabel("Application rules (checked means disabled)");
        final CheckBox appGlobal = addCheckBox("Disable all Agent operations", false);
        final CheckBox appScreenshot = addCheckBox("Disable screenshots", false);
        final CheckBox appInput = addCheckBox("Disable input", false);

        addSectionLabel("Main Activity rules (checked means disabled)");
        final CheckBox activityGlobal = addCheckBox("Disable all Agent operations", false);
        final CheckBox activityScreenshot = addCheckBox("Disable screenshots", false);
        final CheckBox activityInput = addCheckBox("Disable input", false);

        addSectionLabel("Agent intents (checked means disabled)");
        final CheckBox modifyContent = addCheckBox("Modify content", false);
        final CheckBox postContent = addCheckBox("Post content", false);
        final CheckBox deleteContent = addCheckBox("Delete content", false);
        final CheckBox accountIncentive = addCheckBox("Account incentive", false);

        View.OnClickListener previewListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    SaepPolicy.Config config = readPolicyConfig(versionInput,
                            appGlobal, appScreenshot, appInput,
                            activityGlobal, activityScreenshot, activityInput,
                            modifyContent, postContent, deleteContent, accountIncentive);
                    setOutput(SaepPolicy.summarizePolicy(
                            SaepPolicy.createDynamicPolicy(config)));
                } catch (IllegalArgumentException e) {
                    setOutput("Invalid policy: " + e.getMessage());
                }
            }
        };
        Button preview = addActionButton("Preview Dynamic Policy");
        preview.setOnClickListener(previewListener);
        final Button update = addActionButton("Update Dynamic Policy");
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SaepPolicy.Config config;
                try {
                    config = readPolicyConfig(versionInput,
                            appGlobal, appScreenshot, appInput,
                            activityGlobal, activityScreenshot, activityInput,
                            modifyContent, postContent, deleteContent, accountIncentive);
                } catch (IllegalArgumentException e) {
                    setOutput("Invalid policy: " + e.getMessage());
                    return;
                }
                runSaepCall("Updating dynamic policy", new SaepTask() {
                    @Override
                    public SaepFrameworkClient.CallResult run() {
                        return client.updateDynamicPolicy(config);
                    }
                });
            }
        });
        preview.performClick();
    }

    private void showLocalLogs() {
        currentScreen = "logs";
        beginScreen("Local Log Query", "Reads recent audit entries with documented filters.");
        addBackButton();
        final EditText startTime = addEditText("Start time in milliseconds (0 = any)", "0", true);
        final EditText endTime = addEditText("End time in milliseconds (0 = any)", "0", true);
        final EditText targetIntent = addEditText("Target intent (optional)", "", false);
        final EditText targetActivity = addEditText("Target Activity (optional)", "", false);
        Button query = addActionButton("Query Local Logs");
        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final long startTimeMs;
                final long endTimeMs;
                try {
                    startTimeMs = readTimestamp(startTime, "Start time");
                    endTimeMs = readTimestamp(endTime, "End time");
                } catch (IllegalArgumentException e) {
                    setOutput("Invalid query: " + e.getMessage());
                    return;
                }
                final String intent = normalizeOptional(targetIntent);
                final String activity = normalizeOptional(targetActivity);
                runSaepCall("Querying local logs", new SaepTask() {
                    @Override
                    public SaepFrameworkClient.CallResult run() {
                        return client.queryLogs(startTimeMs, endTimeMs, intent, activity);
                    }
                });
            }
        });
        setOutput("No logs queried yet.");
    }

    private void showTrustedAgents() {
        currentScreen = "agents";
        beginScreen("Trusted Agents", "List trusted agents and inspect one agent UUID in detail.");
        addBackButton();
        Button list = addActionButton("List Trusted Agents");
        final EditText uuidInput = addEditText("Agent UUID", "", false);
        Button detail = addActionButton("Query Agent Detail");
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runSaepCall("Listing trusted agents", new SaepTask() {
                    @Override
                    public SaepFrameworkClient.CallResult run() {
                        return client.listRegisteredAgents();
                    }
                });
            }
        });
        detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String agentUuid = uuidInput.getText().toString().trim();
                runSaepCall("Querying agent UUID " + agentUuid, new SaepTask() {
                    @Override
                    public SaepFrameworkClient.CallResult run() {
                        return client.getAgentInfo(agentUuid);
                    }
                });
            }
        });
        setOutput("List agents first, then paste an agent UUID to query details.");
    }

    private void beginScreen(String title, String subtitle) {
        screenGeneration++;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColorCompat(R.color.saep_background));
        root.setPadding(dp(18), dp(22), dp(18), dp(18));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColorCompat(R.color.saep_primary_dark));
        titleView.setTextSize(30);
        titleView.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        root.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(getColorCompat(R.color.saep_muted));
        subtitleView.setTextSize(15);
        subtitleView.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_panel);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        output = new TextView(this);
        output.setTextColor(getColorCompat(R.color.saep_text));
        output.setTextSize(14);
        output.setTypeface(Typeface.MONOSPACE);
        output.setPadding(0, dp(16), 0, 0);
        content.addView(output, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private void addNavButton(String label, View.OnClickListener listener) {
        Button button = addActionButton(label);
        button.setOnClickListener(listener);
    }

    private void addBackButton() {
        Button button = makeSecondaryButton("Back");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHome();
            }
        });
        content.addView(button, 0, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private Button addActionButton(String label) {
        Button button = makeButton(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        content.addView(button, Math.max(0, content.getChildCount() - 1), params);
        return button;
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_primary_button);
        button.setMinHeight(dp(48));
        return button;
    }

    private Button makeSecondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(getColorCompat(R.color.saep_primary_dark));
        button.setTextSize(15);
        button.setBackgroundResource(R.drawable.bg_secondary_button);
        button.setMinHeight(dp(44));
        return button;
    }

    private CheckBox addCheckBox(String label, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setChecked(checked);
        checkBox.setTextColor(getColorCompat(R.color.saep_text));
        checkBox.setTextSize(15);
        content.addView(checkBox, Math.max(0, content.getChildCount() - 1),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return checkBox;
    }

    private EditText addEditText(String hint, String value, boolean numeric) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setInputType(numeric
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT);
        editText.setText(value);
        editText.setTextColor(getColorCompat(R.color.saep_text));
        editText.setHintTextColor(getColorCompat(R.color.saep_muted));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        content.addView(editText, Math.max(0, content.getChildCount() - 1), params);
        return editText;
    }

    private void addSectionLabel(String label) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextColor(getColorCompat(R.color.saep_primary_dark));
        textView.setTextSize(16);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setPadding(0, dp(12), 0, dp(4));
        content.addView(textView, Math.max(0, content.getChildCount() - 1),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void runSaepCall(String inProgress, final SaepTask task) {
        if (callInProgress) {
            setOutput("A SAEP request is already in progress.");
            return;
        }
        callInProgress = true;
        setOutput(inProgress + "...");
        final int generation = screenGeneration;
        final TextView targetOutput = output;
        try {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if (generation != screenGeneration) {
                        finishSaepCall(generation, targetOutput, null);
                        return;
                    }

                    SaepFrameworkClient.CallResult result;
                    try {
                        result = task.run();
                    } catch (RuntimeException e) {
                        result = SaepFrameworkClient.CallResult.failure(
                                e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                    finishSaepCall(generation, targetOutput, result);
                }
            });
        } catch (RejectedExecutionException e) {
            callInProgress = false;
            setOutput("Unable to start the SAEP request.");
        }
    }

    private void finishSaepCall(final int generation, final TextView targetOutput,
            final SaepFrameworkClient.CallResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callInProgress = false;
                if (result == null || isDestroyed() || generation != screenGeneration
                        || output != targetOutput) {
                    return;
                }
                setOutput((result.success ? "Success\n\n" : "Unavailable or failed\n\n")
                        + result.message);
            }
        });
    }

    private void setOutput(String text) {
        output.setText(text);
    }

    private SaepPolicy.Config readPolicyConfig(EditText versionInput,
            CheckBox appGlobal, CheckBox appScreenshot, CheckBox appInput,
            CheckBox activityGlobal, CheckBox activityScreenshot, CheckBox activityInput,
            CheckBox modifyContent, CheckBox postContent, CheckBox deleteContent,
            CheckBox accountIncentive) {
        SaepPolicy.Config config = new SaepPolicy.Config();
        config.version = readPolicyVersion(versionInput);
        config.appGlobalDisable = appGlobal.isChecked();
        config.appScreenshotDisable = appScreenshot.isChecked();
        config.appInputDisable = appInput.isChecked();
        config.activityGlobalDisable = activityGlobal.isChecked();
        config.activityScreenshotDisable = activityScreenshot.isChecked();
        config.activityInputDisable = activityInput.isChecked();
        config.modifyContentDisable = modifyContent.isChecked();
        config.postContentDisable = postContent.isChecked();
        config.deleteContentDisable = deleteContent.isChecked();
        config.accountIncentiveDisable = accountIncentive.isChecked();
        return config;
    }

    private static int readPolicyVersion(EditText versionInput) {
        String value = versionInput.getText().toString().trim();
        if (value.length() == 0) {
            throw new IllegalArgumentException("policy_version must be positive");
        }
        try {
            int version = Integer.parseInt(value);
            if (version <= 0) {
                throw new IllegalArgumentException("policy_version must be positive");
            }
            return version;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("policy_version must be a positive integer");
        }
    }

    private static String normalizeOptional(EditText input) {
        String value = input.getText().toString().trim();
        return value;
    }

    private static long readTimestamp(EditText input, String label) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a non-negative integer");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getColorCompat(int colorResourceId) {
        return getColor(colorResourceId);
    }

    private interface SaepTask {
        SaepFrameworkClient.CallResult run();
    }
}
