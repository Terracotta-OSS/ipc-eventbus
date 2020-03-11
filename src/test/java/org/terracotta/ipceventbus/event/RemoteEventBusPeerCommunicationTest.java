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
package org.terracotta.ipceventbus.event;

import com.jayway.awaitility.Awaitility;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RemoteEventBusPeerCommunicationTest {
  @Test
  public void bus_can_communicate_events() throws Throwable {
    int port = create(15000, 15500);
    RecordingEventListener listener = new RecordingEventListener();

    final EventBusServer peer1 = new EventBusServer.Builder()
        .id("peer1")
        .listen(port)
        .build();

    peer1.on(listener);
    peer1.on(new EventListenerSniffer("peer1"));

    final EventBusClient peer2 = new EventBusClient.Builder()
        .id("peer2")
        .connect(port)
        .build();

    peer2.on(listener);
    peer2.on(new EventListenerSniffer("peer2"));

    peer1.trigger("action");
    Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> 2 == listener.userEvents);

    peer2.trigger("action");
    Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> 4 == listener.userEvents);

    peer1.close();
    peer2.close();
  }

  private int create(int startPort, int endPort) {
    return IntStream.range(startPort, endPort)
        .filter(port -> {
          try {
            new ServerSocket(port).close();
            return true;
          } catch (IOException ex) {
            return false;
          }
        })
        .findFirst()
        .orElseThrow(() -> new RuntimeException("no free port found"));
  }
}
