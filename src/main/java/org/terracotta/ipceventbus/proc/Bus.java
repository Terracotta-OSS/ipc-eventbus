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

import org.terracotta.ipceventbus.event.EventBusServer;
import org.terracotta.ipceventbus.event.EventListenerSniffer;

import java.lang.management.ManagementFactory;

/**
 * @author Mathieu Carbou
 */
public final class Bus {

  private static final EventBusServer bus;

  static {
    int port = 0;
    if (System.getProperty("ipc.bus.port") != null) {
      port = Integer.parseInt(System.getProperty("ipc.bus.port"));
    }
    String pid = getCurrentPid();
    if (isDebug()) {
      System.out.println("[" + Boot.class.getSimpleName() + "] Child PID: " + pid);
      System.out.println("[" + Boot.class.getSimpleName() + "] Starting EventBus Server " + pid + " on 0.0.0.0:" + port + "...");
    }
    bus = new EventBusServer.Builder()
        .listen(port)
        .id(pid)
        .build();
    if (isDebug()) {
      bus.on(new EventListenerSniffer(pid));
    }
  }

  public static EventBusServer get() {
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
