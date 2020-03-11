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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public class AnyProcessBuilder<T extends AnyProcess> {

  File workingDir = new File(".");
  InputStream pipeStdin;
  OutputStream pipeStdout;
  OutputStream pipeStderr;
  boolean redirectStderr;
  Map<String, String> env = new LinkedHashMap<>(System.getenv());
  boolean recordStdout;
  boolean recordStderr;
  List<String> command = new ArrayList<>();
  boolean debug;

  public AnyProcessBuilder<T> debug() {
    this.debug = true;
    return this;
  }

  public final AnyProcessBuilder<T> command(String... command) {
    this.command = new ArrayList<>(Arrays.asList(command));
    return this;
  }

  public final AnyProcessBuilder<T> pipeStdin(InputStream pipeStdin) {
    this.pipeStdin = pipeStdin;
    return this;
  }

  public final AnyProcessBuilder<T> pipeStdin() {
    return pipeStdin(System.in);
  }

  public final AnyProcessBuilder<T> pipeStdout(OutputStream pipeStdout) {
    this.pipeStdout = pipeStdout;
    return this;
  }

  public final AnyProcessBuilder<T> pipeStdout() {
    return pipeStdout(System.out);
  }

  public final AnyProcessBuilder<T> pipeStderr(OutputStream pipeStderr) {
    this.pipeStderr = pipeStderr;
    return this;
  }

  public final AnyProcessBuilder<T> pipeStderr() {
    return pipeStderr(System.err);
  }

  public final AnyProcessBuilder<T> workingDir(File workingDirectory) {
    this.workingDir = workingDirectory;
    return this;
  }

  public final AnyProcessBuilder<T> redirectStderr() {
    this.redirectStderr = true;
    return this;
  }

  public final AnyProcessBuilder<T> env(Map<String, String> newEnv) {
    this.env = new LinkedHashMap<>(newEnv);
    return this;
  }

  public final AnyProcessBuilder<T> env(String key, String value) {
    this.env.put(key, value);
    return this;
  }

  public final AnyProcessBuilder<T> recordStdout() {
    this.recordStdout = true;
    return this;
  }

  public final AnyProcessBuilder<T> recordStderr() {
    this.recordStderr = true;
    return this;
  }

  public final T build() {
    buildCommand();
    Process process = createProcess();
    T t = wrap(process, command);
    if (debug) {
      System.out.println("[" + t.getCurrentPid() + "] Started process " + t.getPid() + ": " + t.getCommandLine());
    }
    return t;
  }

  protected void buildCommand() {
  }

  @SuppressWarnings("unchecked")
  protected T wrap(Process process, List<String> command) {
    return (T) new AnyProcess(process, pipeStdout, pipeStderr, pipeStdin, recordStdout, recordStderr, command, workingDir);
  }

  private Process createProcess() {
    if (command.isEmpty()) {
      throw new IllegalArgumentException("Missing command");
    }
    java.lang.ProcessBuilder builder = new java.lang.ProcessBuilder()
        .command(command)
        .directory(workingDir)
        .redirectErrorStream(redirectStderr);
    Map<String, String> processEnv = builder.environment();
    processEnv.clear();
    processEnv.putAll(this.env);
    try {
      return builder.start();
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to start " + command, e);
    }
  }

}
