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

package org.terracotta.ipceventbus.proc.draft;

import org.terracotta.ipceventbus.event.EventBusClient;
import org.terracotta.ipceventbus.event.EventListenerSniffer;

import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * @author Mathieu Carbou
 */
public class EchoEventCli {
  public static void main(String[] args) throws InterruptedException, IOException {
    System.out.println(getCurrentPid());
    EventBusClient eb = new EventBusClient.Builder()
        .id(getCurrentPid())
        .connect(49475)
        .build();
    eb.on(new EventListenerSniffer());
    eb.trigger("echo", "Hello world!");
    eb.trigger("echo", "Hello you!");
    eb.trigger("exit");
  }

  private static String getCurrentPid() {
    try {
      return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    } catch (Exception ignored) {
      return null;
    }
  }

}
