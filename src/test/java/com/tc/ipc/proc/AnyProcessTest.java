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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class AnyProcessTest {

  @Test
  public void test_launch_process() throws InterruptedException {
    AnyProcess anyProcess = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 2; echo $VAR")
        .env("VAR", "Hello world!")
        .pipeStdout()
        .pipeStderr()
        .recordStdout()
        .recordStderr()
        .build();

    try {
      anyProcess.exitValue();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
    }

    try {
      anyProcess.getRecordedStdout();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Process not terminated.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStderr();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Process not terminated.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStdoutText();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Process not terminated.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStderrText();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Process not terminated.", e.getMessage());
    }

    assertTrue(anyProcess.getPid() > 0);
    assertTrue(anyProcess.isRunning());
    assertEquals(Arrays.asList("bash", "-c", "sleep 2; echo $VAR"), anyProcess.getCommand());
    assertEquals(new File("."), anyProcess.getWorkingDirectory());
    assertEquals(0, anyProcess.waitFor());
    assertEquals(0, anyProcess.exitValue());
    assertEquals("", anyProcess.getRecordedStderrText());
    assertEquals("Hello world!\n", anyProcess.getRecordedStdoutText());
  }

  @Test
  public void test_launch_process_without_collecting() throws InterruptedException {
    AnyProcess anyProcess = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 2; echo $VAR")
        .env("VAR", "Hello world!")
        .build();

    try {
      anyProcess.getRecordedStdout();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Stdout not recorded.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStderr();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Stderr not recorded.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStdoutText();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Stdout not recorded.", e.getMessage());
    }

    try {
      anyProcess.getRecordedStderrText();
      fail();
    } catch (Exception e) {
      assertEquals(IllegalThreadStateException.class, e.getClass());
      assertEquals("Stderr not recorded.", e.getMessage());
    }

    assertEquals(0, anyProcess.waitFor());
  }

  @Test(timeout = 1000)
  public void test_destroy() throws InterruptedException {
    AnyProcess proc = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 3; echo $VAR")
        .pipeStdout()
        .pipeStderr()
        .env("VAR", "Hello world!")
        .build();

    Thread.sleep(500);
    proc.destroy();
    assertTrue(proc.isDestroyed());

    // 143 = return code when SIGKILL
    assertEquals(143, proc.exitValue());
    assertEquals(143, proc.waitFor());
  }

  @Test
  public void using_future() throws InterruptedException, TimeoutException, ExecutionException {
    AnyProcess proc = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 2; echo $VAR")
        .pipeStdout()
        .pipeStderr()
        .env("VAR", "Hello world!")
        .build();

    assertEquals(0, proc.getFuture().get(3, TimeUnit.SECONDS).intValue());
    assertTrue(proc.getFuture().isDone());
    assertFalse(proc.getFuture().isCancelled());
    assertEquals(0, proc.getFuture().get().intValue());
  }

  @Test(timeout = 1000)
  public void test_destroy_future() throws InterruptedException {
    AnyProcess proc = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 3; echo $VAR")
        .pipeStdout()
        .pipeStderr()
        .env("VAR", "Hello world!")
        .build();

    Thread.sleep(500);
    proc.getFuture().cancel(true);
    assertTrue(proc.isDestroyed());

    assertTrue(proc.getFuture().isDone());
    assertTrue(proc.getFuture().isCancelled());

    // 143 = return code when SIGKILL
    assertEquals(143, proc.exitValue());
    assertEquals(143, proc.waitFor());

    try {
      proc.getFuture().get();
      fail();
    } catch (Exception e) {
      assertEquals(CancellationException.class, e.getClass());
    }
  }

  @Test
  public void test_waitForTime() throws InterruptedException, TimeoutException {
    AnyProcess proc = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 3; echo $VAR")
        .pipeStdout()
        .pipeStderr()
        .env("VAR", "Hello world!")
        .build();

    try {
      proc.waitForTime(1, TimeUnit.SECONDS);
      fail();
    } catch (TimeoutException ignored) {
    }

    assertEquals(0, proc.waitForTime(3, TimeUnit.SECONDS));
  }

  @Test
  public void pipe_stdin() throws InterruptedException, TimeoutException, ExecutionException, IOException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);
    AnyProcess proc = AnyProcess.newBuilder()
        .command("bash", "-c", "read input && echo $input")
        .pipeStdout()
        .pipeStderr()
        .pipeStdin(in)
        .recordStderr()
        .recordStdout()
        .build();
    out.write("Hello World!\n".getBytes());
    out.close();
    assertEquals(0, proc.waitFor());
    assertEquals("Hello World!\n", proc.getRecordedStdoutText());
  }

}
