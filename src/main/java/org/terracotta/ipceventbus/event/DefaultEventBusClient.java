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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Mathieu Carbou
 */
class DefaultEventBusClient extends DefaultEventBus implements EventBusClient {

  private final AtomicReference<Socket> socket = new AtomicReference<Socket>();
  private ObjectOutputStream outputStream;
  private ObjectInputStream inputStream;
  private Thread receiver;

  DefaultEventBusClient(Socket socket, ErrorListener listener, Listeners initialListeners) {
    this(socket.getLocalAddress().getHostName() + ":" + socket.getLocalPort(), socket, listener, initialListeners);
  }

  DefaultEventBusClient(String uuid, Socket socket, ErrorListener listener) {
    this(uuid, socket, listener, new Listeners());
  }

  DefaultEventBusClient(String uuid, Socket socket, ErrorListener listener, Listeners initialListeners) {
    super(uuid, listener, initialListeners);
    this.socket.set(socket);
    try {
      this.outputStream = new ObjectOutputStream(socket.getOutputStream());
      this.inputStream = new ObjectInputStream(socket.getInputStream());
    } catch (IOException e) {
      close();
      throw new EventBusIOException("Bad socket: " + socket + " : " + e.getMessage(), e);
    }
    final CountDownLatch receiving = new CountDownLatch(1);
    receiver = new Thread("reader@" + getId()) {
      @Override
      public void run() {
        receiving.countDown();
        while (!Thread.currentThread().isInterrupted() && !isClosed()) {
          Event event = null;
          try {
            event = (Event) inputStream.readObject();
          } catch (IOException | ClassNotFoundException e) {
            sendLocal(new DefaultEvent(DefaultEventBusClient.this.getId(), "eventbus.client.error", e));

            close();
          }
          if (event != null && "eventbus.event".equals(event.getName())) {
            sendLocal(event.getData(Event.class));
          }
        }
      }
    };
    receiver.setDaemon(true);
    receiver.start();
    try {
      receiving.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    if (!isClosed()) {
      Socket s = socket.get();
      if (s != null && socket.compareAndSet(s, null)) {
        try {
          s.close();
        } catch (IOException ignored) {
        }
        outputStream = null;
        inputStream = null;
        if (receiver != null) {
          receiver.interrupt();
          receiver = null;
        }
        sendLocal(new DefaultEvent(getId(), "eventbus.client.disconnect"));
      }
    }
  }

  @Override
  public int getServerPort() {
    return socket.get().getPort();
  }

  @Override
  public String getServerHost() {
    return socket.get().getInetAddress().getHostName();
  }

  @Override
  public boolean isClosed() {
    return socket.get() == null || socket.get().isClosed();
  }

  @Override
  public void trigger(String name, Object data) {
    Assert.legalEventName(name);
    Assert.notInternalName(name);
    Event event = new DefaultEvent(getId(), name, data);
    sendLocal(event);
    sendRemote(event);
  }

  void sendRemote(Event event) {
    if (!isClosed()) {
      try {
        outputStream.writeObject(new DefaultEvent(getId(), "eventbus.event", event));
        outputStream.flush();
      } catch (IOException e) {
        close();
      }
    }
  }

  @Override
  public String toString() {
    return EventBusClient.class.getSimpleName() + ":" + getId();
  }

}
