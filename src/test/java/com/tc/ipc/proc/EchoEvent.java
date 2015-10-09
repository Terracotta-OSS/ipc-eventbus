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

import com.tc.ipc.event.Event;
import com.tc.ipc.event.EventBus;
import com.tc.ipc.event.EventListener;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public class EchoEvent {
  public static void main(String[] args) throws Exception {
    // print pid
    System.out.println("EchoEvent: pid=" + getCurrentPid());

    // print args
    System.out.println("EchoEvent: args=" + Arrays.asList(args));

    final EventBus bus = Bus.get();

    // listen to all events, even system events
    bus.on(new EventListener() {
      @Override
      public void onEvent(Event e) {
        System.out.println("EchoEvent: " + e);
      }
    });

    // ping-pong
    bus.on("ping", new EventListener() {
      @Override
      public void onEvent(Event e) {
        bus.trigger("pong", e.getData());
      }
    });

    Thread.sleep(1000);
  }

  private static String getCurrentPid() {
    try {
      return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    } catch (Exception ignored) {
      return null;
    }
  }

}
