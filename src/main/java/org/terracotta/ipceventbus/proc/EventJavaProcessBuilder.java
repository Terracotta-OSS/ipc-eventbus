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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;

/**
 * @author Mathieu Carbou
 */
public class EventJavaProcessBuilder<T extends EventJavaProcess> extends JavaProcessBuilder<T> {

  int port;

  public EventJavaProcessBuilder<T> port(int port) {
    this.port = port;
    return this;
  }

  public EventJavaProcessBuilder<T> randomPort() {
    return port(0);
  }

  @Override
  protected void buildCommand() {
    if (port > 0) {
      try {
        new ServerSocket(port).close();
      } catch (IOException e) {
        throw new IllegalArgumentException("Cannot listen on port " + port + ": " + e.getMessage(), e);
      }
    } else {
      while (true) {
        port = 1025 + new Random().nextInt(64000);
        try {
          new ServerSocket(port).close();
          break;
        } catch (IOException ignored) {
        }
      }
    }
    addClasspath(Bus.class);
    addJvmProp("ipc.bus.port", "" + port);
    if (debug) {
      addJvmProp("ipc.bus.debug", "true");
    }
    if (mainClass != null) {
      addJvmProp("ipc.bus.mainClass", mainClass);
      mainClass(Boot.class.getName());
    }

    super.buildCommand();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T wrap(Process process, List<String> command) {
    return (T) new EventJavaProcess(
        process,
        pipeStdout, pipeStderr, pipeStdin, recordStdout, recordStderr, command, workingDir,
        javaHome, javaExecutable, jvmArgs, classpath, mainClass, arguments, jvmProps,
        port, debug);
  }

}
