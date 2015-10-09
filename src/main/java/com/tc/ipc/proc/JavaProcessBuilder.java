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

package com.tc.ipc.proc;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public class JavaProcessBuilder<T extends JavaProcess> extends AnyProcessBuilder<T> {

  String mainClass;
  File javaHome = new File(System.getProperty("java.home"));
  File javaExecutable = findJavaExecutable(javaHome);
  List<String> jvmArgs = new ArrayList<String>();
  List<File> classpath = new ArrayList<File>();
  List<String> arguments = new ArrayList<String>();
  Map<String, String> jvmProps = new LinkedHashMap<String, String>();

  public final JavaProcessBuilder<T> mainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  public final JavaProcessBuilder<T> mainClass(Class<?> mainClass) {
    mainClass(mainClass.getName());
    return addClasspath(mainClass);
  }

  public final JavaProcessBuilder<T> javaHome(File javaHome) {
    this.javaHome = javaHome;
    return this;
  }

  public final JavaProcessBuilder<T> javaExecutable(File javaExecutable) {
    this.javaExecutable = javaExecutable;
    return this;
  }

  public final JavaProcessBuilder<T> jvmArgs(List<String> jvmArgs) {
    this.jvmArgs = jvmArgs;
    return this;
  }

  public final JavaProcessBuilder<T> addJvmArg(String arg) {
    this.jvmArgs.add(arg);
    return this;
  }

  public final JavaProcessBuilder<T> jvmProps(Map<String, String> jvmProps) {
    this.jvmProps = new LinkedHashMap<String, String>(jvmProps);
    return this;
  }

  public final JavaProcessBuilder<T> addJvmProp(String key, String val) {
    this.jvmProps.put(key, val);
    return this;
  }

  public final JavaProcessBuilder<T> classpath(List<File> classpath) {
    this.classpath = classpath;
    return this;
  }

  public final JavaProcessBuilder<T> addClasspath(URL location) {
    try {
      return addClasspath(new File(location.toURI()));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(location.toString());
    }
  }

  public final JavaProcessBuilder<T> addClasspath(File location) {
    if (!this.classpath.contains(location)) {
      this.classpath.add(location);
    }
    return this;
  }

  public final JavaProcessBuilder<T> addClasspath(Class<?> enclosingJar) {
    return addClasspath(enclosingJar.getProtectionDomain().getCodeSource().getLocation());
  }

  public final JavaProcessBuilder<T> arguments(List<String> args) {
    this.arguments = args;
    return this;
  }

  public final JavaProcessBuilder<T> arguments(String... args) {
    this.arguments = Arrays.asList(args);
    return this;
  }

  public final JavaProcessBuilder<T> addArgument(String arg) {
    this.arguments.add(arg);
    return this;
  }

  @Override
  protected void buildCommand() {
    if (mainClass == null) {
      throw new IllegalArgumentException("Missing main class");
    }
    if (javaHome == null || !javaHome.isDirectory()) {
      throw new IllegalArgumentException("Bad JAVA_HOME: " + javaHome);
    }
    if (javaExecutable == null || !javaExecutable.isFile()) {
      throw new IllegalArgumentException("Bad Java executable: " + javaExecutable);
    }

    command.add(javaExecutable.getAbsolutePath());
    command.addAll(jvmArgs);

    if (!classpath.isEmpty()) {
      for (File path : classpath) {
        if (!path.exists()) {
          throw new IllegalArgumentException("Classpath entry does not exist: " + path);
        }
      }
      command.add("-classpath");
      StringBuilder cp = new StringBuilder(classpath.get(0).getAbsolutePath());
      String sep = AnyProcess.isWindows() ? ";" : ":";
      for (int i = 1; i < classpath.size(); i++) {
        cp.append(sep).append(classpath.get(i));
      }
      command.add(cp.toString());
    }

    for (Map.Entry<String, String> entry : jvmProps.entrySet()) {
      command.add("-D" + entry.getKey() + "=" + entry.getValue());
    }

    command.add(mainClass);
    command.addAll(arguments);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T wrap(Process process, List<String> command) {
    return (T) new JavaProcess(
        process,
        pipeStdout, pipeStderr, pipeStdin, recordStdout, recordStderr, command, workingDir,
        javaHome, javaExecutable, jvmArgs, classpath, mainClass, arguments, jvmProps);
  }

  private static File findJavaExecutable(File javaHome) {
    File javaBin = new File(javaHome, "bin");
    File javaPlain = new File(javaBin, "java");
    File javaExe = new File(javaBin, "java.exe");
    if (javaPlain.isFile()) return javaPlain;
    if (javaExe.isFile()) return javaExe;
    return null;
  }

}
