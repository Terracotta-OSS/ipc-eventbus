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
class DefaultEventBus implements EventBus {

  private final String uuid;
  private final ErrorListener errorListener;
  protected final Listeners listeners;

  DefaultEventBus(String uuid, ErrorListener errorListener) {
    this(uuid, errorListener, new Listeners());
  }

  DefaultEventBus(String uuid, ErrorListener errorListener, Listeners initialListeners) {
    this.uuid = uuid;
    this.errorListener = errorListener;
    this.listeners = new Listeners(initialListeners);
  }

  @Override
  public String getId() {
    return uuid;
  }

  @Override
  public void on(String event, EventListener listener) {
    Assert.legalEventName(event);
    listeners.on(event).add(listener);
  }

  @Override
  public void on(EventListener listener) {
    listeners.on("").add(listener);
  }

  @Override
  public void trigger(String name) {
    trigger(name, null);
  }

  @Override
  public void trigger(String name, Object data) {
    Assert.legalEventName(name);
    Assert.notInternalName(name);
    Event event = new DefaultEvent(getId(), name, data);
    sendLocal(event);
  }

  void sendLocal(Event event) {
    for (EventListener listener : listeners.on(event.getName())) {
      try {
        listener.onEvent(event);
      } catch (Throwable e) {
        errorListener.onError(event, listener, e);
      }
    }
    for (EventListener listener : listeners.on("")) {
      try {
        listener.onEvent(event);
      } catch (Throwable e) {
        errorListener.onError(event, listener, e);
      }
    }
  }

  @Override
  public void unbind(String event) {
    Assert.legalEventName(event);
    Assert.notInternalName(event);
    listeners.removeAll(event);
  }

  @Override
  public void unbind(EventListener listener) {
    listeners.removeAll(listener);
  }

  @Override
  public void unbind(String event, EventListener listener) {
    Assert.legalEventName(event);
    listeners.remove(event, listener);
  }

  @Override
  public String toString() {
    return EventBus.class.getSimpleName() + ":" + getId();
  }

}
