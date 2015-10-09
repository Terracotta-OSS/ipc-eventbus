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

package com.tc.ipc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class MultiplexOutputStream extends OutputStream {

  private List<OutputStream> streams = new ArrayList<OutputStream>(2);

  public MultiplexOutputStream() {
  }

  public MultiplexOutputStream(OutputStream os) {
    addOutputStream(os);
  }

  public List<OutputStream> getOutputStreams() {
    return streams;
  }

  public MultiplexOutputStream addOutputStream(OutputStream os) {
    streams.add(os);
    return this;
  }

  public boolean isEmpty() {
    return streams.isEmpty();
  }

  public int streamCount() {
    return streams.size();
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() throws IOException {
    IOException ioe = null;
    for (OutputStream stream : streams) {
      try {
        stream.flush();
      } catch (IOException e) {
        ioe = e;
      }
    }
    if (ioe != null) {
      throw ioe;
    }
  }

  @Override
  public void write(int b) throws IOException {
    IOException ioe = null;
    for (OutputStream stream : streams) {
      try {
        stream.write(b);
      } catch (IOException e) {
        ioe = e;
      }
    }
    if (ioe != null) {
      throw ioe;
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    IOException ioe = null;
    for (OutputStream stream : streams) {
      try {
        stream.write(b);
      } catch (IOException e) {
        ioe = e;
      }
    }
    if (ioe != null) {
      throw ioe;
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    IOException ioe = null;
    for (OutputStream stream : streams) {
      try {
        stream.write(b, off, len);
      } catch (IOException e) {
        ioe = e;
      }
    }
    if (ioe != null) {
      throw ioe;
    }
  }

}
