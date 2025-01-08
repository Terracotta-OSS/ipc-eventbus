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

package org.terracotta.ipceventbus.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Pipe implements Closeable {

  private static final Logger LOGGER = Logger.getLogger(Pipe.class.getName());

  private volatile Thread pipe;

  public Pipe(String name, final InputStream in, final OutputStream out) {
    this(name, in, out, 8192);
  }

  public Pipe(String name, final InputStream in, final OutputStream out, final int bufferSize) {
    this.pipe = new Thread(name) {
      @Override
      public void run() {
        byte[] buffer = new byte[bufferSize];
        int len;
        try {
          while (!Thread.currentThread().isInterrupted() && (len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
            out.flush();
          }
        } catch (InterruptedIOException ignored) {
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Error reading/writing streams: " + e.getMessage(), e);
        } finally {
          close();
        }
      }
    };
    pipe.setDaemon(true);
    pipe.start();
  }

  public void waitFor() throws InterruptedException {
    Thread t = this.pipe;
    if (t != null) {
      t.join();
      close();
    }
  }

  /**
   * Close the pipe, but not the underlying streams!
   */
  public synchronized void close() {
    if (pipe != null) {
      Thread t = pipe;
      pipe = null;
      t.interrupt();
    }
  }
}
