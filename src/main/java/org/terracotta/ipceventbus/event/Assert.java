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

/**
 * @author Mathieu Carbou
 */
final class Assert {

  static void legalEventName(String name) {
    if (name == null) throw new NullPointerException("Event name is null");
    if (name.length() == 0) throw new IllegalArgumentException(name);
  }

  static void notInternalName(String name) {
    if (name.startsWith("eventbus.")) throw new IllegalArgumentException(name);
  }

  static void opened(RemoteEventBus eventBus) {
    if (eventBus.isClosed()) throw new IllegalStateException(eventBus + " is closed.");
  }
}
