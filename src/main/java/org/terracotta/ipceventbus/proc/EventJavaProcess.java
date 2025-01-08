/*
 * Copyright 2015 Terracotta, Inc., a Software AG company.
 * Copyright IBM Corp. 2024, 2025
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

import org.terracotta.ipceventbus.event.EventBusClient;
import org.terracotta.ipceventbus.event.EventBusServer;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.event.EventListenerSniffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public final class EventJavaProcess extends JavaProcess {

  private volatile EventBusServer eventBus;

  public EventJavaProcess(Process process,
                          OutputStream pipeStdout, OutputStream pipeStderr, InputStream pipeStdin, boolean collectStdout, boolean collectStderr, List<String> command, File workingDir,
                          File javaHome, File javaExecutable, List<String> jvmArgs, List<File> classpath, String mainClass, List<String> arguments, Map<String, String> jvmProperties,
                          boolean debug, EventBusServer eventBusServer) {
    super(process,
        pipeStdout, pipeStderr, pipeStdin, collectStdout, collectStderr, command, workingDir,
        javaHome, javaExecutable, jvmArgs, classpath, mainClass, arguments, jvmProperties);
    String pid = getCurrentPid();
    // try to connect
    this.eventBus = eventBusServer;
    if (debug) {
      eventBus.on(new EventListenerSniffer(pid));
    }
  }

  @Override
  protected void onDestroyed() {
    if (eventBus != null) {
      eventBus.trigger("process.destroyed");
    }
    close();
  }

  @Override
  protected void onTerminated() {
    if (eventBus != null) {
      eventBus.trigger("process.exited");
    }
    close();
  }

  private void close() {
    try {
      if (eventBus != null && eventBus instanceof EventBusClient && !((EventBusClient) eventBus).isClosed()) {
        ((EventBusClient) eventBus).close();
      }
    } catch (IOException ignored) {
    }
  }

  public final boolean isEventBusConnected() {
    return eventBus.getClientCount() > 0;
  }

  public final String getEventBusServerHost() {
    return eventBus.getServerHost();
  }

  public final int getEventBusServerPort() {
    return eventBus.getServerPort();
  }

  public final String getEventBusId() {
    return eventBus.getId();
  }

  public final void on(String event, org.terracotta.ipceventbus.event.EventListener listener) {
    eventBus.on(event, listener);
  }

  public final void unbind(String event) {
    eventBus.unbind(event);
  }

  public final void on(EventListener listener) {
    eventBus.on(listener);
  }

  public final void unbind(EventListener listener) {
    eventBus.unbind(listener);
  }

  public final void unbind(String event, EventListener listener) {
    eventBus.unbind(event, listener);
  }

  public final void trigger(String name) {
    eventBus.trigger(name);
  }

  public final void trigger(String name, Object data) {
    eventBus.trigger(name, data);
  }

  public static EventJavaProcessBuilder<? extends EventJavaProcess> newBuilder() {
    return new EventJavaProcessBuilder<EventJavaProcess>();
  }

}
