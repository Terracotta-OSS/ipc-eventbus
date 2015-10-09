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

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * @author Mathieu Carbou
 */
public interface EventBusServer extends RemoteEventBus {

  final class Builder extends BaseBuilder<Builder> {

    int port = Integer.parseInt(System.getProperty("ipc.bus.port", "56789"));
    String address = "0.0.0.0";

    public Builder() {
      errorListener = new PrintingErrorListener();
    }

    public Builder bind(String address) {
      this.address = address;
      return this;
    }

    public Builder listen(int port) {
      this.port = port;
      return this;
    }

    public Builder listenRandom() {
      port = 0;
      return this;
    }

    @Override
    public EventBusServer build() throws EventBusException {
      try {
        ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket();
        serverSocket.bind(new InetSocketAddress(address, port));
        return new DefaultEventBusServer(busId != null ? busId : (serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort()), serverSocket, errorListener);
      } catch (IOException e) {
        throw new EventBusIOException("Cannot bind on " + address + ":" + port + " : " + e.getMessage(), e);
      }
    }

  }
}
