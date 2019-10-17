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

package org.terracotta.ipceventbus;

import java.util.concurrent.TimeUnit;

public class ThreadUtil {
  // Java does not provide a guarantee that Thread.sleep will actually sleep long enough.
  // In fact, on Windows, it does not sleep for long enough.
  // This method keeps sleeping until the full time has passed.
  //
  // Using System.nanoTime (accurate to 1 micro-second or better) in lieu of System.currentTimeMillis (on Windows
  // accurate to ~16ms), the inaccuracy of which compounds when invoked multiple times, as in this method.

  public static void minimumSleep(long millis) throws InterruptedException {
    long nanos = TimeUnit.MILLISECONDS.toNanos(millis);
    long start = System.nanoTime();

    while (true) {
      long nanosLeft = nanos - (System.nanoTime() - start);

      if (nanosLeft <= 0) {
        break;
      }

      try {
        TimeUnit.NANOSECONDS.sleep(nanosLeft);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }
    }
  }
}
