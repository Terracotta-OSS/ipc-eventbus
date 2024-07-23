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

package org.terracotta.ipceventbus.proc;

import com.jayway.awaitility.Awaitility;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.ipceventbus.ThreadUtil;
import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.draft.SocketClient;
import org.terracotta.ipceventbus.proc.draft.SocketServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        .pipeStdout()
        .pipeStderr()
        .recordStdout()
        .recordStderr()
        .build();

    assertFalse(process.isEventBusConnected());

    assertEquals(1, process.waitFor());
    assertEquals("", process.getRecordedStdoutText());
    assertTrue(process.getRecordedStderrText().contains("java.lang.ClassNotFoundException: org.terracotta.ipceventbus.proc.EchoEvent"));
  }

  @Test(timeout = 10_000)
  public void child_server_socket() throws Throwable {

    final int[] ports = getRandomPorts(1);
    System.out.println("ports: " + Arrays.toString(ports));

    Thread serverThread = new Thread("server") {
      @Override
      public void run() {
        try {
          SocketServer.main(String.valueOf(ports[0]));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    serverThread.start();

    Thread clientThread = new Thread("client") {
      @Override
      public void run() {
        try {
          ThreadUtil.minimumSleep(1000); // gives time for the server socket to bind and accept
          SocketClient.main(String.valueOf(ports[0]));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    clientThread.start();

    serverThread.join();
    clientThread.join();

    String cp = new File(SocketServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
    final int[] ports2 = getRandomPorts(1);
    System.out.println("ports: " + Arrays.toString(ports));

    AnyProcess serverProc = AnyProcess.newBuilder()
        .debug()
        .command("java", "-classpath", cp, SocketServer.class.getName(), String.valueOf(ports2[0]))
        .pipeStdout()
        .pipeStderr()
        .build();

    ThreadUtil.minimumSleep(1000); // gives time for the server socket to bind and accept
    AnyProcess clientProc = AnyProcess.newBuilder()
        .debug()
        .command("java", "-classpath", cp, SocketClient.class.getName(), String.valueOf(ports2[0]))
        .pipeStdout()
        .pipeStderr()
        .build();

    serverProc.waitFor();
    clientProc.waitFor();

    final int[] ports3 = getRandomPorts(1);
    System.out.println("ports: " + Arrays.toString(ports));

    JavaProcess serverJava = JavaProcess.newBuilder()
        .mainClass(SocketServer.class)
        .addClasspath(SocketServer.class)
        .arguments(String.valueOf(ports3[0]))
        .pipeStdout()
        .pipeStderr()
        .debug()
        .build();

    ThreadUtil.minimumSleep(1000); // gives time for the server socket to bind and accept
    JavaProcess clientJava = JavaProcess.newBuilder()
        .mainClass(SocketClient.class)
        .addClasspath(SocketClient.class)
        .arguments(String.valueOf(ports3[0]))
        .pipeStdout()
        .pipeStderr()
        .debug()
        .build();

    serverJava.waitFor();
    clientJava.waitFor();
  }

  @Test(timeout = 10_000)
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

    Awaitility.waitAtMost(5, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return process.isEventBusConnected();
      }
    });

    process.trigger("ping", "hello");
    process.trigger("process.exit");

    assertEquals(0, process.waitFor());
    assertEquals("", process.getRecordedStderrText());
  }

  @Test(timeout = 10_000)
  public void be_alerted_of_process_end() throws Throwable {

    EventJavaProcess process = EventJavaProcess.newBuilder()
        .mainClass(EchoEvent2.class) // set main class to start and add it to classpath
        .pipeStdout() // echo stdout
        .pipeStderr() // echo stderr
        .debug() // activate debug mode for ipc eventbus
        .build();

    Awaitility.waitAtMost(5, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return process.isEventBusConnected();
      }
    });

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

  private static int[] getRandomPorts(int n) {
    int[] ports = new int[n];
    for (int port = 2000, i = 0; i < ports.length && port < 65000; port++) {
      try {
        new ServerSocket(port).close();
        ports[i++] = port;
      } catch (IOException ignored) {
      }
    }
    return ports;
  }

}
