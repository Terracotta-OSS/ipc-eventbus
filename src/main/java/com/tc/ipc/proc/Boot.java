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

package com.tc.ipc.proc;

import com.tc.ipc.event.Event;
import com.tc.ipc.event.EventListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mathieu Carbou
 */
public final class Boot {
  public static void main(String[] args) throws Throwable {

    if (System.getProperty("ipc.bus.mainClass") == null) {
      throw new IllegalStateException("No main class specified");
    }

    Class<?> mainClass = Class.forName(System.getProperty("ipc.bus.mainClass"));

    if (Bus.isDebug()) {
      System.out.println("[" + Boot.class.getSimpleName() + "] Starting " + mainClass.getName() + "...");
    }

    final AtomicBoolean firedExiting = new AtomicBoolean();

    Bus.get().on("process.exit", new EventListener() {
      @Override
      @SuppressFBWarnings("DM_EXIT")
      public void onEvent(Event e) {
        if (firedExiting.compareAndSet(false, true)) {
          Bus.get().trigger("process.exiting", Bus.getCurrentPid());
        }
        System.exit(e.getData(Integer.class, 0));
      }
    });

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (firedExiting.compareAndSet(false, true)) {
          Bus.get().trigger("process.exiting", Bus.getCurrentPid());
        }
      }
    });

    try {
      mainClass.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{args});
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } finally {
      Thread current = Thread.currentThread();
      for (Thread thread : Thread.getAllStackTraces().keySet()) {
        if (!thread.isDaemon() && current != thread) {
          try {
            thread.join();
          } catch (InterruptedException ignored) {
          }
        }
      }
      if (firedExiting.compareAndSet(false, true)) {
        Bus.get().trigger("process.exiting");
      }
    }
  }
}
