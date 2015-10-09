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

import java.io.PrintStream;

/**
 * @author Mathieu Carbou
 */
public class EventListenerSniffer implements EventListener {

  private final PrintStream out;
  private final String category;

  public EventListenerSniffer() {
    this("", System.out);
  }

  public EventListenerSniffer(PrintStream out) {
    this("", out);
  }

  public EventListenerSniffer(String category) {
    this(category, System.out);
  }

  public EventListenerSniffer(String category, PrintStream out) {
    this.out = out;
    this.category = category.length() > 0 ? (" [" + category + "]") : category;
  }

  @Override
  public void onEvent(Event e) throws Throwable {
    out.println(System.currentTimeMillis() + category + " [" + Thread.currentThread().getName() + "] " + e.getName() + "@" + e.getSource() + " at " + e.getTimestamp() + (e.getData() == null ? "" : (" - " + e.getData())));
  }
}
