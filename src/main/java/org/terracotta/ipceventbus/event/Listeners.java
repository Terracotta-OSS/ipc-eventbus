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

package org.terracotta.ipceventbus.event;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
final class Listeners {

  private final ConcurrentMap<String, Collection<EventListener>> index;

  Listeners(Listeners original) {
    this.index = new ConcurrentHashMap<>(original.index.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> new CopyOnWriteArrayList<>(e.getValue()))));
  }

  Listeners() {
    this.index = new ConcurrentHashMap<>();
  }

  public Collection<EventListener> on(String event) {
    this.index.putIfAbsent(event, new CopyOnWriteArrayList<EventListener>());
    return this.index.get(event);
  }

  public void removeAll(String event) {
    index.remove(event);
  }

  public void removeAll(EventListener listener) {
    for (Collection<EventListener> listeners : index.values()) {
      listeners.remove(listener);
    }
  }

  public void remove(String event, EventListener listener) {
    Collection<EventListener> listeners = index.get(event);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }
}
