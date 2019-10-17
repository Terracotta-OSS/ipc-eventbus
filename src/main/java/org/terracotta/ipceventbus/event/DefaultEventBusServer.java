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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Mathieu Carbou
 */
final class DefaultEventBusServer extends DefaultEventBus implements EventBusServer {

  private final Collection<DefaultEventBusClient> clients = new LinkedList<DefaultEventBusClient>();
  private final ReadWriteLock clientsLock = new ReentrantReadWriteLock();
  private final AtomicReference<ServerSocket> serverSocket = new AtomicReference<ServerSocket>();
  private Thread acceptor;

  DefaultEventBusServer(String uuid, ServerSocket serverSocket, final ErrorListener errorListener) {
    super(uuid, errorListener);
    this.serverSocket.set(serverSocket);
    final EventListener listener = new EventListener() {
      @Override
      public void onEvent(Event e) {
        sendLocal(e);
        if (!e.isUserEvent() && "eventbus.client.disconnect".equals(e.getName())) {
          for (DefaultEventBusClient cli : getClients()) {
            if (cli.getId().equals(e.getSource())) {
              clientsLock.writeLock().lock();
              try {
                clients.remove(cli);
              } finally {
                clientsLock.writeLock().unlock();
              }
              break;
            }
          }
        }
      }
    };
    final CountDownLatch listening = new CountDownLatch(1);
    acceptor = new Thread("client-acceptor") {
      @Override
      public void run() {
        listening.countDown();
        while (!Thread.currentThread().isInterrupted() && !isClosed()) {
          try {
            Socket socket = DefaultEventBusServer.this.serverSocket.get().accept();
            InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
            DefaultEventBusClient client = new DefaultEventBusClient(address.getHostName() + ":" + address.getPort(), socket, errorListener);
            client.on(listener);
            clientsLock.writeLock().lock();
            try {
              clients.add(client);
            } finally {
              clientsLock.writeLock().unlock();
            }
            sendLocal(new DefaultEvent(DefaultEventBusServer.this.getId(), "eventbus.client.connect", client.getId()));
          } catch (IOException e) {
            close();
          }
        }
      }
    };
    acceptor.setDaemon(true);
    acceptor.start();
    try {
      listening.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  @Override
  public int getServerPort() {
    return serverSocket.get().getLocalPort();
  }

  @Override
  public boolean isClosed() {
    return serverSocket.get() == null || serverSocket.get().isClosed();
  }

  @Override
  public void close() {
    if (!isClosed()) {
      ServerSocket ss = this.serverSocket.get();
      if (ss != null && this.serverSocket.compareAndSet(ss, null)) {
        try {
          ss.close();
        } catch (IOException ignored) {
        }
        acceptor.interrupt();
        acceptor = null;
        for (DefaultEventBusClient client : getClients()) {
          client.close();
        }
        sendLocal(new DefaultEvent(getId(), "eventbus.server.close"));
      }
    }
  }

  @Override
  public synchronized void trigger(String name, Object data) {
    Assert.legalEventName(name);
    Assert.notInternalName(name);
    Event event = new DefaultEvent(getId(), name, data);
    sendLocal(event);
    sendRemote(event);
  }

  void sendRemote(Event event) {
    for (DefaultEventBusClient client : clients) {
      client.sendRemote(event);
    }
  }

  @Override
  public String toString() {
    return EventBusServer.class.getSimpleName() + ":" + getId();
  }

  private Collection<DefaultEventBusClient> getClients() {
    clientsLock.readLock().lock();
    try {
      return new ArrayList<DefaultEventBusClient>(clients);
    } finally {
      clientsLock.readLock().unlock();
    }
  }

}
