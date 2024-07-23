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

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractEventBusTest<T extends EventBus> {

  RecordingErrorListener errorListener = new RecordingErrorListener();
  T eventBus;

  @After
  public void checkErrors() throws Throwable {
    if (errorListener.errors > 0) {
      throw errorListener.exceptions.get(0);
    }
  }

  @Test
  public void bus_has_id() {
    assertNotNull(eventBus.getId());
  }

  @Test
  public void bus_has_toString() {
    assertTrue(eventBus.toString().startsWith("EventBus"));
    assertTrue(eventBus.toString().endsWith(":" + eventBus.getId()));
  }

  @Test
  public void listen_on_event() {

    final AtomicInteger listened = new AtomicInteger();

    eventBus.on("action", new EventListener() {
      @Override
      public void onEvent(Event e) {
        assertEquals(eventBus.getId(), e.getSource());
        assertEquals("action", e.getName());
        assertEquals("data", e.getData());
        assertEquals("data", e.getData(String.class));
        assertTrue(System.currentTimeMillis() - 1000 < e.getTimestamp());
        assertTrue(System.currentTimeMillis() >= e.getTimestamp());
        assertEquals("Event{name='action', source=" + e.getSource() + ", data=data}", e.toString());
        listened.incrementAndGet();
      }
    });

    eventBus.trigger("action", "data");

    assertEquals(1, listened.get());
  }

  @Test
  public void listen_on_all_event() {

    final AtomicInteger listened = new AtomicInteger();

    eventBus.on(new EventListener() {
      @Override
      public void onEvent(Event e) {
        if (e.isUserEvent()) {
          assertEquals(eventBus.getId(), e.getSource());
          assertEquals("data", e.getData());
          assertEquals("data", e.getData(String.class));
          listened.incrementAndGet();
        }
      }
    });

    eventBus.trigger("action1", "data");
    eventBus.trigger("action2", "data");

    assertEquals(2, listened.get());
  }

  @Test
  public void trigger_no_data() {

    final AtomicInteger listened = new AtomicInteger();

    eventBus.on(new EventListener() {
      @Override
      public void onEvent(Event e) {
        if (e.isUserEvent()) {
          assertEquals(eventBus.getId(), e.getSource());
          assertNull(e.getData());
          listened.incrementAndGet();
        }
      }
    });

    eventBus.trigger("action1");

    assertEquals(1, listened.get());
  }

  @Test
  public void unbind_listener() {
    RecordingEventListener listener = new RecordingEventListener();
    eventBus.on(listener);
    eventBus.trigger("action1");
    assertEquals(1, listener.events);
    eventBus.unbind(listener);
    eventBus.trigger("action1");
    assertEquals(1, listener.events);
  }

  @Test
  public void unbind_listener_from_event() {
    RecordingEventListener listener = new RecordingEventListener();
    eventBus.on(listener);
    eventBus.on("action1", listener);
    eventBus.trigger("action1");
    assertEquals(2, listener.events);
    eventBus.unbind("action1", listener);
    eventBus.trigger("action1");
    assertEquals(3, listener.events);
  }

  @Test
  public void unbind_from_event() {
    RecordingEventListener listener = new RecordingEventListener();
    eventBus.on("action1", listener);
    eventBus.trigger("action1");
    assertEquals(1, listener.events);
    eventBus.unbind("action1");
    eventBus.trigger("action1");
    assertEquals(1, listener.events);
  }

  @Test(expected = NullPointerException.class)
  public void error_trigger_null_event_name() {
    eventBus.trigger(null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void listener_error_rethrown_by_default_for_wildcard_listeners() {
    EventBus eb = new EventBus.Builder().build();
    eb.on(new EventListener() {
      @Override
      public void onEvent(Event e) {
        throw new UnsupportedOperationException();
      }
    });
    eb.trigger("event");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void listener_error_rethrown_by_default_for_event_listeners() {
    EventBus eb = new EventBus.Builder().build();
    eb.on("event", new EventListener() {
      @Override
      public void onEvent(Event e) {
        throw new UnsupportedOperationException();
      }
    });
    eb.trigger("event");
  }

  @Test(expected = IllegalArgumentException.class)
  public void error_trigger_empty_event_name() {
    eventBus.trigger("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void error_trigger_internal_event_name() {
    eventBus.trigger("eventbus.something");
  }

  @Test(expected = NullPointerException.class)
  public void error_on_null_event_name() {
    eventBus.on(null, new EventListenerAdapter());
  }

  @Test(expected = IllegalArgumentException.class)
  public void error_on_empty_event_name() {
    eventBus.on("", new EventListenerAdapter());
  }

}
