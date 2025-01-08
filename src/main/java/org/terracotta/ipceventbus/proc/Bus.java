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
import org.terracotta.ipceventbus.event.EventListenerSniffer;

import java.lang.management.ManagementFactory;

/**
 * @author Mathieu Carbou
 */
public final class Bus {

  private static final EventBusClient bus;

  static {
    String host = System.getProperty("ipc.bus.host");
    int port = Integer.parseInt(System.getProperty("ipc.bus.port"));
    String pid = getCurrentPid();

    if (isDebug()) {
      System.out.println("[" + Boot.class.getSimpleName() + "] Child PID: " + pid);
      System.out.println("[" + Boot.class.getSimpleName() + "] Connecting EventBus Client " + pid + " to " + host + ":" + port + "...");
    }
    bus = new EventBusClient.Builder()
        .connect(host, port)
        .id(pid)
        .build();
    if (isDebug()) {
      bus.on(new EventListenerSniffer(pid));
    }
  }

  public static EventBusClient get() {
    return bus;
  }

  public static boolean isDebug() {
    return System.getProperty("ipc.bus.debug") != null;
  }

  static String getCurrentPid() {
    try {
      return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    } catch (Exception ignored) {
      return null;
    }
  }

}
