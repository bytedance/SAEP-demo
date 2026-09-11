@rem
@rem Copyright (c) 2026 ByteDance Ltd. and/or its affiliates.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem     https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo gradle-wrapper.jar is not checked in. Install Gradle 8.9 locally or generate the wrapper jar outside this task scope. 1>&2
  exit /b 1
)

java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
