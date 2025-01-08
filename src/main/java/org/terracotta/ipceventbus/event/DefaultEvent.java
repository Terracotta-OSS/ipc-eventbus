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

package org.terracotta.ipceventbus.event;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
class DefaultEvent implements Serializable, Event {

  private static final long serialVersionUID = -4856946361193249489L;

  private final String source;
  private final String name;
  private final long timestamp = System.currentTimeMillis();
  private final Object data;

  DefaultEvent(String source, String name) {
    this(source, name, null);
  }

  DefaultEvent(String source, String name, Object data) {
    Assert.legalEventName(name);
    this.source = source;
    this.name = name;
    this.data = data;
  }

  @Override
  public boolean isUserEvent() {
    return !name.startsWith("eventbus.");
  }

  @Override
  public String getSource() {
    return source;
  }


  @Override
  public String getName() {
    return name;
  }


  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public Object getData() {
    return data;
  }

  @Override
  public <T> T getData(Class<T> type) {
    return type.cast(getData());
  }

  @Override
  public <T> T getData(Class<T> type, T defaultValue) {
    T t = getData(type);
    return t == null ? defaultValue : t;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Event{");
    sb.append("name='").append(name).append('\'');
    sb.append(", source=").append(source);
    sb.append(", data=").append(data);
    sb.append('}');
    return sb.toString();
  }

}
