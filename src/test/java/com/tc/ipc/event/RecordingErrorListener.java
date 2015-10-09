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

package com.tc.ipc.event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
class RecordingErrorListener implements ErrorListener {

  int errors;
  List<Throwable> exceptions = new ArrayList<Throwable>();
  List<Event> events = new ArrayList<Event>();

  @Override
  public synchronized void onError(Event event, EventListener listener, Throwable e) {
    errors++;
    exceptions.add(e);
    events.add(event);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RecordingErrorListener{");
    sb.append("errors=").append(errors);
    sb.append(", exceptions=").append(exceptions);
    sb.append(", events=").append(events);
    sb.append('}');
    return sb.toString();
  }
}
