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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
class RecordingEventListener implements EventListener {

  int events;
  int userEvents;
  int systemEvents;
  List<String> sources = new ArrayList<String>();
  List<String> names = new ArrayList<String>();

  @Override
  public synchronized void onEvent(Event e) throws Throwable {
    events++;
    if (e.isUserEvent()) userEvents++;
    else systemEvents++;
    names.add(e.getName());
    sources.add(e.getSource());
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RecordingEventListener{");
    sb.append("events=").append(events);
    sb.append(", userEvents=").append(userEvents);
    sb.append(", systemEvents=").append(systemEvents);
    sb.append(", sources=").append(sources);
    sb.append(", names=").append(names);
    sb.append('}');
    return sb.toString();
  }
}
