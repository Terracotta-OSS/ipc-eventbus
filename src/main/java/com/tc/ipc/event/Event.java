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

/**
 * @author Mathieu Carbou
 */
public interface Event {

  boolean isUserEvent();

  /**
   * @return The ID of the {@link EventBus} where the event comes from. Same as {@link EventBus#getId()}
   */
  String getSource();

  /**
   * @return The event name triggered
   */
  String getName();

  /**
   * @return The millisecond timestamp of this event
   */
  long getTimestamp();

  /**
   * @return The raw data of this event, if any
   */
  Object getData();

  /**
   * Convert the raw data in given type
   *
   * @param type Teh class of wanted type
   * @param <T>  The type to cast the raw data into
   * @return The catsed raw data
   */
  <T> T getData(Class<T> type);

  <T> T getData(Class<T> type, T defaultValue);
}
