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

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author Mathieu Carbou
 */
public interface EventBusClient extends RemoteEventBus {

  String getServerHost();

  final class Builder extends BaseBuilder<Builder> {

    InetSocketAddress endpoint;

    public Builder() {
      errorListener = new PrintingErrorListener();
    }

    public Builder connect(String host, int port) {
      endpoint = new InetSocketAddress(host, port);
      return this;
    }

    public Builder connect(int port) {
      return connect("localhost", port);
    }

    @Override
    public EventBusClient build() throws EventBusException {
      if (endpoint == null) {
        connect(System.getProperty("ipc.bus.host", "localhost"), Integer.parseInt(System.getProperty("ipc.bus.port", "56789")));
      }
      try {
        Socket socket = SocketFactory.getDefault().createSocket();
        socket.connect(endpoint);
        return busId == null ? new DefaultEventBusClient(socket, errorListener, listeners) : new DefaultEventBusClient(busId, socket, errorListener, listeners);
      } catch (IOException e) {
        throw new EventBusIOException("Bad endpoint: " + endpoint.getHostName() + ":" + endpoint.getPort() + " : " + e.getMessage(), e);
      }
    }

  }
}
