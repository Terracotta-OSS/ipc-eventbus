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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author Mathieu Carbou
 */
public class SHook {
  public static void main(String[] args) throws IOException {
    Socket socket = new Socket("localhost", 1234);
    final OutputStream outputStream = socket.getOutputStream();

        /*Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // run nc -l 1234
                try {
                    Socket socket = new Socket("localhost", 1234);
                    socket.getOutputStream().write("bye\n".getBytes());
                    socket.flush();
                } catch (IOException uglyBigCatch) {
                    uglyBigCatch.printStackTrace();
                }
            }
        });*/

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          outputStream.write("bye\n".getBytes());
          outputStream.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
