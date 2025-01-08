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

package org.terracotta.ipceventbus.event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class RemoteEventBusTest extends AbstractEventBusTest<EventBusClient> {

  EventBusServer peer;

  @Before
  public void init() {
    peer = new EventBusServer.Builder()
        .onError(errorListener)
        .listenRandom()
        .build();
    peer.on(new EventListenerSniffer("server"));

    eventBus = new EventBusClient.Builder()
        .onError(errorListener)
        .connect(peer.getServerPort())
        .build();
    eventBus.on(new EventListenerSniffer("eventBus"));
  }

  @After
  public void close() throws IOException {
    peer.close();
    eventBus.close();
  }

  @Test
  public void server_has_toString() {
    assertEquals("EventBusServer:0.0.0.0:" + peer.getServerPort(), peer.toString());
  }

  @Test
  public void bus_has_host() {
    assertEquals("localhost", eventBus.getServerHost());
  }

  @Test
  public void bus_has_port() {
    assertEquals(peer.getServerPort(), eventBus.getServerPort());
  }

  @Test
  public void bus_has_isClosed() {
    assertFalse(peer.isClosed());
    assertFalse(eventBus.isClosed());
  }
}
