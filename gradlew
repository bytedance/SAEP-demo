#!/bin/sh
#
# Copyright (c) 2026 ByteDance Ltd. and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "gradle-wrapper.jar is not checked in. Install Gradle 8.9 locally or generate the wrapper jar outside this task scope." >&2
  exit 1
fi

exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
