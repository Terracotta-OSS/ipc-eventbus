/*
 * Copyright 2015 Terracotta, Inc., a Software AG company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.ipceventbus.proc;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public class JavaProcess extends AnyProcess {

  private final File javaHome;
  private final File javaExecutable;
  private final List<String> jvmArgs;
  private final Map<String, String> jvmProperties;
  private final List<File> classpath;
  private final String mainClass;
  private final List<String> arguments;

  public JavaProcess(Process process,
                     OutputStream pipeStdout, OutputStream pipeStderr, InputStream pipeStdin, boolean collectStdout, boolean collectStderr, List<String> command, File workingDir,
                     File javaHome, File javaExecutable, List<String> jvmArgs, List<File> classpath, String mainClass, List<String> arguments, Map<String, String> jvmProperties) {
    super(process, pipeStdout, pipeStderr, pipeStdin, collectStdout, collectStderr, command, workingDir);
    this.javaHome = javaHome;
    this.javaExecutable = javaExecutable;
    this.jvmArgs = Collections.unmodifiableList(jvmArgs);
    this.classpath = Collections.unmodifiableList(classpath);
    this.mainClass = mainClass;
    this.arguments = Collections.unmodifiableList(arguments);
    this.jvmProperties = Collections.unmodifiableMap(jvmProperties);
  }

  public final Map<String, String> getJvmProperties() {
    return jvmProperties;
  }

  public final File getJavaHome() {
    return javaHome;
  }

  public final File getJavaExecutable() {
    return javaExecutable;
  }

  public final List<String> getJvmArgs() {
    return jvmArgs;
  }

  public final List<File> getClasspath() {
    return classpath;
  }

  public final String getMainClass() {
    return mainClass;
  }

  public final List<String> getArguments() {
    return arguments;
  }

  public static JavaProcessBuilder<? extends JavaProcess> newBuilder() {
    return new JavaProcessBuilder<JavaProcess>();
  }
}
