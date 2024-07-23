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

import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class BaseBuilder<T extends BaseBuilder<T>> {

  String busId;
  ErrorListener errorListener = new RethrowingErrorListener();
  Listeners listeners = new Listeners();

  BaseBuilder() {
  }

  /**
   * Identifies a bus. If no ID is given, a generated one will be given.
   *
   * @param busId An identifier to use for this bus
   * @return this builder
   */
  public T id(String busId) {
    this.busId = busId;
    return (T) this;
  }

  /**
   * Register a new listener for an event
   *
   * @param event    The event name
   * @param listener The listener to register
   */
  public T on(String event, EventListener listener) {
    listeners.on(event).add(listener);
    return (T) this;
  }

  /**
   * Register a new listener for all event
   *
   * @param listener The listener to register
   */
  public T on(EventListener listener) {
    listeners.on("").add(listener);
    return (T) this;
  }

  /**
   * Registers an {@link ErrorListener} to handle exceptions thrown by {@link EventListener}. By default, exceptions are rethrown.
   *
   * @param listener The listener to use. Some default implementations are provided.
   * @return this builder
   */
  public T onError(ErrorListener listener) {
    this.errorListener = listener;
    return (T) this;
  }

  public EventBus build() throws EventBusException {
    return new DefaultEventBus(busId != null ? busId : UUID.randomUUID().toString(), errorListener, listeners);
  }

}
