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

package org.terracotta.ipceventbus.proc.draft;

import javax.net.ServerSocketFactory;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Mathieu Carbou
 */
public class SocketServer {
  public static void main(String... args) throws Exception {

    ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket();
    serverSocket.bind(new InetSocketAddress("0.0.0.0", Integer.parseInt(args[0])));
    Socket socket = serverSocket.accept();
    System.out.println("server read from client: " + new ObjectInputStream(socket.getInputStream()).readObject());
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    objectOutputStream.writeObject("hello world!");
    objectOutputStream.flush();
    socket.close();
    serverSocket.close();
  }
}
