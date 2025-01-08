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

package org.terracotta.ipceventbus.proc;

import org.terracotta.ipceventbus.ThreadUtil;

import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public class EchoFail {
  public static void main(String[] args) throws InterruptedException {
    System.out.println(System.getenv("VAR"));
    System.out.println(System.getProperty("my.prop"));
    for (String arg : args) {
      System.out.println(arg);
    }
    ThreadUtil.minimumSleep(1000);
    fail("message");
  }
}
