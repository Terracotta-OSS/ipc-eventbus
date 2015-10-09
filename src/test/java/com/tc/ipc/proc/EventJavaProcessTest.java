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
import com.tc.ipc.proc.draft.SocketClient;
import com.tc.ipc.proc.draft.SocketServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class EventJavaProcessTest {

  @Test
  public void test_launch_failing_ipc_process() throws InterruptedException {
    EventJavaProcess process = EventJavaProcess.newBuilder()
        .randomPort()
        .mainClass(EchoEvent.class.getName())
        .recordStdout()
        .recordStderr()
        .build();

    assertFalse(process.isEventBusConnected());

    assertEquals(1, process.waitFor());
    assertEquals("", process.getRecordedStdoutText());
    assertTrue(process.getRecordedStderrText().contains("java.lang.ClassNotFoundException: com.tc.ipc.proc.EchoEvent"));
  }

  @Test(timeout = 10000)
  public void child_server_socket() throws Throwable {
    Thread serverThread = new Thread("server") {
      @Override
      public void run() {
        try {
          SocketServer.main(new String[]{"12345"});
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    };
    serverThread.start();

    Thread clientThread = new Thread("client") {
      @Override
      public void run() {
        try {
          SocketClient.main(new String[]{"12345"});
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    };
    clientThread.start();

    serverThread.join();
    clientThread.join();

    String cp = new File(SocketServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

    AnyProcess serverProc = AnyProcess.newBuilder()
        .command("java", "-classpath", cp, SocketServer.class.getName(), "12345")
        .recordStdout()
        .recordStderr()
        .pipeStdout()
        .pipeStderr()
        .build();

    AnyProcess clientProc = AnyProcess.newBuilder()
        .command("java", "-classpath", cp, SocketClient.class.getName(), "12345")
        .recordStdout()
        .recordStderr()
        .pipeStdout()
        .pipeStderr()
        .build();

    serverProc.waitFor();
    clientProc.waitFor();

    JavaProcess serverJava = JavaProcess.newBuilder()
        .mainClass(SocketServer.class)
        .addClasspath(SocketServer.class)
        .arguments("12345")
        .recordStdout()
        .recordStderr()
        .pipeStdout()
        .pipeStderr()
        .build();

    JavaProcess clientJava = JavaProcess.newBuilder()
        .mainClass(SocketClient.class)
        .addClasspath(SocketClient.class)
        .arguments("12345")
        .recordStdout()
        .recordStderr()
        .pipeStdout()
        .pipeStderr()
        .build();

    serverJava.waitFor();
    clientJava.waitFor();
  }

  @Test(timeout = 5000)
  public void sample_usage_launch_ipc_process() throws Throwable {

    EventJavaProcess process = EventJavaProcess.newBuilder()
        .randomPort()
        .mainClass(EchoEvent.class.getName())
        .addClasspath(EchoEvent.class)
        .arguments("one", "two")
        .recordStdout()
        .recordStderr()
        .pipeStdout()
        .pipeStderr()
        .debug()
        .build();

    System.out.println("Test: PID: " + process.getCurrentPid());
    System.out.println("Test: CHILD PID: " + process.getPid());

    assertTrue(process.isEventBusConnected());

    process.trigger("ping", "hello");
    process.trigger("process.exit");

    assertEquals(0, process.waitFor());
    assertEquals("", process.getRecordedStderrText());
  }

  @Test(timeout = 5000)
  public void be_alerted_of_process_end() throws Throwable {

    EventJavaProcess process = EventJavaProcess.newBuilder()
        .mainClass(EchoEvent2.class) // set main class to start and add it to classpath
        .pipeStdout() // echo stdout
        .pipeStderr() // echo stderr
        .debug() // activate debug mode for ipc eventbus
        .build();

    assertTrue(process.isEventBusConnected());

    final CountDownLatch latch = new CountDownLatch(3);

    process.on("process.exiting", new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        System.out.println("Exiting...");
        latch.countDown();
      }
    });

    process.on("process.exited", new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        System.out.println("Exited.");
        latch.countDown();
      }
    });

    process.on("pong", new EventListener() {
      @Override
      public void onEvent(Event e) throws Throwable {
        System.out.println(e.getData());
        latch.countDown();
      }
    });

    process.trigger("ping", "hello");
    process.trigger("process.exit");

    process.waitFor();

    latch.await();
  }

}
