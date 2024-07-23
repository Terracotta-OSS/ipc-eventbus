/*
 * Copyright 2015 Terracotta, Inc., a Software AG company.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.ipceventbus.ThreadUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class PipeTest {

  @Test
  public void pipe_test() throws Exception {
    ByteArrayOutputStream collected = new ByteArrayOutputStream();
    PipedInputStream in = new PipedInputStream();
    final PipedOutputStream out = new PipedOutputStream(in);
    new Thread() {
      @Override
      public void run() {
        try {
          for (int i = 1; i <= 5; i++) {
            out.write(("put-" + i + "\n").getBytes());
            ThreadUtil.minimumSleep(500);
          }
          out.close();
        } catch (Exception e) {
          e.printStackTrace();
          fail();
        }
      }
    }.start();
    Pipe pipe = new Pipe("name", in, collected);
    pipe.waitFor();
    assertEquals("put-1\nput-2\nput-3\nput-4\nput-5\n", new String(collected.toByteArray()));
  }

  @Test
  public void pipe_interruption() throws Exception {
    ByteArrayOutputStream collected = new ByteArrayOutputStream();
    PipedInputStream in = new PipedInputStream();
    final PipedOutputStream out = new PipedOutputStream(in);
    final CountDownLatch brake = new CountDownLatch(2);
    new Thread() {
      {
        setDaemon(true);
      }

      @Override
      public void run() {
        try {
          for (int i = 1; i <= 5; i++) {
            out.write(("a").getBytes());
            out.flush();
            brake.countDown();
            ThreadUtil.minimumSleep(500);
          }
          fail();
        } catch (Exception e) {
          assertEquals(IOException.class, e.getClass());
        }
      }
    }.start();
    Pipe pipe = new Pipe("name", in, collected);
    brake.await();
    pipe.close();
    assertEquals("aa", new String(collected.toByteArray()));
  }

}
