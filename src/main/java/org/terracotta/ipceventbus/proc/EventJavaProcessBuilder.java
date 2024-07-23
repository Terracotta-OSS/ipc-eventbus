/*
 * Copyright 2015 Terracotta, Inc., a Software AG company.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.ipceventbus.proc;

import org.terracotta.ipceventbus.event.EventBusServer;
import org.terracotta.ipceventbus.event.EventListener;

/**
 * @author Mathieu Carbou
 */
public class EventJavaProcessBuilder<T extends EventJavaProcess> extends JavaProcessBuilder<T> {

  final EventBusServer.Builder eventBusBuilder = new EventBusServer.Builder();

  public EventJavaProcessBuilder<T> port(int port) {
    eventBusBuilder.listen(port);
    return this;
  }

  public EventJavaProcessBuilder<T> randomPort() {
    eventBusBuilder.listenRandom();
    return this;
  }

  /**
   * Register a new listener for an event
   *
   * @param event    The event name
   * @param listener The listener to register
   */
  public EventJavaProcessBuilder<T> on(String event, EventListener listener) {
    eventBusBuilder.on(event, listener);
    return this;
  }

  /**
   * Register a new listener for all event
   *
   * @param listener The listener to register
   */
  public EventJavaProcessBuilder<T> on(EventListener listener) {
    eventBusBuilder.on(listener);
    return this;
  }

  @Override
  public T build() {
    EventBusServer eventBusServer = eventBusBuilder.build();

    addJvmProp("ipc.bus.host", "localhost");
    addJvmProp("ipc.bus.port", Integer.toString(eventBusServer.getServerPort()));
    addClasspath(Bus.class);
    if (debug) {
      addJvmProp("ipc.bus.debug", "true");
    }
    if (mainClass != null) {
      addJvmProp("ipc.bus.mainClass", mainClass);
      mainClass(Boot.class.getName());
    }

    buildCommand();
    return (T) new EventJavaProcess(createProcess(),
            pipeStdout, pipeStderr, pipeStdin, recordStdout, recordStderr, command, workingDir,
            javaHome, javaExecutable, jvmArgs, classpath, mainClass, arguments, jvmProps,
            debug, eventBusServer);
  }
}
