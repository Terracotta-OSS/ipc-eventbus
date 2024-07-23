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

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class MultiplexTest {

  @Test
  public void can_multiplex_streams() throws Exception {
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    final MultiplexOutputStream plex = new MultiplexOutputStream()
        .addOutputStream(baos1)
        .addOutputStream(baos2)
        .addOutputStream(System.out);
    assertEquals(3, plex.streamCount());
    assertFalse(plex.isEmpty());
    assertTrue(plex.getOutputStreams().contains(System.out));
    assertTrue(plex.getOutputStreams().contains(baos1));
    assertTrue(plex.getOutputStreams().contains(baos2));
    Thread writer = new Thread() {
      @Override
      public void run() {
        try {
          for (int i = 1; i <= 5; i++) {
            plex.write(("put-" + i + "\n").getBytes());
          }
          plex.close();
        } catch (Exception e) {
          e.printStackTrace();
          fail();
        }
      }
    };
    writer.start();
    writer.join();
    assertEquals("put-1\nput-2\nput-3\nput-4\nput-5\n", new String(baos1.toByteArray()));
    assertEquals("put-1\nput-2\nput-3\nput-4\nput-5\n", new String(baos2.toByteArray()));
  }

}
