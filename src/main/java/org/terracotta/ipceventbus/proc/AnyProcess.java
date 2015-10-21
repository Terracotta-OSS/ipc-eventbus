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

package org.terracotta.ipceventbus.proc;

import org.terracotta.ipceventbus.io.MultiplexOutputStream;
import org.terracotta.ipceventbus.io.Pipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class AnyProcess extends Process {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private final long pid;
  private final Process process;
  private volatile boolean running = true;
  private volatile boolean destroyed;
  private final FutureTask<Integer> future;
  private final Collection<Pipe> pipes = new ArrayList<Pipe>(3);
  private final ByteArrayOutputStream recordedStdout;
  private final ByteArrayOutputStream recordedStderr;
  private final List<String> command;
  private final File workingDirectory;
  private boolean stdoutStreamDisabled;
  private boolean stderrStreamDisabled;

  AnyProcess(Process process, OutputStream pipeStdout, OutputStream pipeStderr, InputStream pipeStdin, boolean collectStdout, boolean collectStderr, List<String> command, File workingDirectory) {
    this.process = process;
    this.pid = getPid(process);
    this.command = Collections.unmodifiableList(command);
    this.workingDirectory = workingDirectory;

    if (pipeStdin != null) {
      this.pipes.add(new Pipe("Process Pipe stdin@" + this.pid, pipeStdin, getOutputStream()));
    }

    // multiplex process stdout
    {
      MultiplexOutputStream stdoutPlex = new MultiplexOutputStream();
      if (pipeStdout != null) {
        stdoutPlex.addOutputStream(pipeStdout);
      }
      if (collectStdout) {
        this.recordedStdout = new ByteArrayOutputStream();
        stdoutPlex.addOutputStream(this.recordedStdout);
      } else {
        this.recordedStdout = null;
      }
      if (!stdoutPlex.isEmpty()) {
        this.pipes.add(new Pipe("Process Pipe stdout@" + this.pid, getInputStream(), stdoutPlex));
        this.stdoutStreamDisabled = true;
      }
    }

    // multiplex process stderr
    {
      MultiplexOutputStream stderrPlex = new MultiplexOutputStream();
      if (pipeStderr != null) {
        stderrPlex.addOutputStream(pipeStderr);
      }
      if (collectStderr) {
        this.recordedStderr = new ByteArrayOutputStream();
        stderrPlex.addOutputStream(this.recordedStderr);
      } else {
        this.recordedStderr = null;
      }
      if (!stderrPlex.isEmpty()) {
        this.pipes.add(new Pipe("Process Pipe stderr@" + this.pid, getErrorStream(), stderrPlex));
        this.stderrStreamDisabled = true;
      }
    }

    // starts future
    {
      this.future = new FutureTask<Integer>(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
          int r = AnyProcess.this.process.waitFor();
          processFinished();
          return r;
        }
      }) {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          if (mayInterruptIfRunning) {
            AnyProcess.this.process.destroy();
          }
          boolean interrupted = false;
          try {
            AnyProcess.this.process.waitFor();
          } catch (InterruptedException ignored) {
            interrupted = true;
          }
          try {
            destroyed = true;
            processFinished();
            return super.cancel(mayInterruptIfRunning);
          } finally {
            if (interrupted) {
              // restore interrupt state
              Thread.currentThread().interrupt();
            }
          }
        }
      };
      Thread thread = new Thread(future, "Process future@" + this.pid);
      thread.setDaemon(true);
      thread.start();
    }
  }

  @Override
  public final OutputStream getOutputStream() {
    if (!isRunning()) throw new IllegalStateException("Not running");
    return this.process.getOutputStream();
  }

  @Override
  public final InputStream getInputStream() {
    if (!isRunning()) throw new IllegalStateException("Not running");
    if (stdoutStreamDisabled) throw new IllegalStateException("no stdout stream available");
    return this.process.getInputStream();
  }

  @Override
  public final InputStream getErrorStream() {
    if (!isRunning()) throw new IllegalStateException("Not running");
    if (stderrStreamDisabled) throw new IllegalStateException("No stderr stream available");
    return this.process.getErrorStream();
  }

  @Override
  public final int waitFor() throws InterruptedException {
    if (!isRunning()) {
      return process.exitValue();
    }
    try {
      return future.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof InterruptedException) {
        throw (InterruptedException) e.getCause();
      }
      if (e.getCause() instanceof Error) {
        throw (Error) e.getCause();
      }
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw new RuntimeException(e.getCause());
    }
  }

  public final int waitForTime(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    if (!isRunning()) {
      return process.exitValue();
    }
    try {
      return future.get(time, unit);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof InterruptedException) {
        throw (InterruptedException) e.getCause();
      }
      if (e.getCause() instanceof Error) {
        throw (Error) e.getCause();
      }
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  public final int exitValue() {
    int c = process.exitValue();
    // if process is not running, an exception is not thrown, thus it means the process is finished
    processFinished();
    return c;
  }

  private void processFinished() {
    if (destroyed) {
      finishPipe(false);
    } else {
      finishPipe(true);
    }
    running = false;
    if (destroyed) {
      onDestroyed();
    } else {
      onTerminated();
    }
  }

  @Override
  public final void destroy() {
    future.cancel(true);
  }

  @Override
  public String toString() {
    return getCommandLine();
  }

  public final List<String> getCommand() {
    return command;
  }

  public final String getCommandLine() {
    StringBuilder sb = new StringBuilder();
    sb.append(command.get(0));
    for (int i = 1; i < command.size(); i++) {
      sb.append(" ").append(command.get(i));
    }
    return sb.toString();
  }

  public final File getWorkingDirectory() {
    return workingDirectory;
  }

  public final long getPid() {
    return pid;
  }

  public final boolean isRunning() {
    return running;
  }

  public final boolean isDestroyed() {
    return destroyed;
  }

  public final Future<Integer> getFuture() {
    return future;
  }

  public final byte[] getRecordedStdout() throws IllegalThreadStateException {
    if (recordedStdout == null) {
      throw new IllegalThreadStateException("Stdout not recorded.");
    }
    if (isRunning()) {
      throw new IllegalThreadStateException("Process not terminated.");
    }
    return recordedStdout.toByteArray();
  }

  public final byte[] getRecordedStderr() throws IllegalThreadStateException {
    if (recordedStderr == null) {
      throw new IllegalThreadStateException("Stderr not recorded.");
    }
    if (isRunning()) {
      throw new IllegalThreadStateException("Process not terminated.");
    }
    return recordedStderr.toByteArray();
  }

  public final String getRecordedStdoutText() throws IllegalThreadStateException {
    return new String(getRecordedStdout(), UTF8);
  }

  public final String getRecordedStderrText() throws IllegalThreadStateException {
    return new String(getRecordedStderr(), UTF8);
  }

  protected void onTerminated() {
  }

  protected void onDestroyed() {
  }

  private void finishPipe(boolean wait) {
    synchronized (pipes) {
      for (Pipe pipe : pipes) {
        try {
          if (wait) {
            try {
              pipe.waitFor();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        } finally {
          pipe.close();
        }
      }
      pipes.clear();
    }
  }

  public static AnyProcessBuilder<? extends AnyProcess> newBuilder() {
    return new AnyProcessBuilder<AnyProcess>();
  }

  private static long getPid(Process process) {
    if (isWindows()) {
      if (isJnaAvailable()) {
        return Jna.getWindowsPid(process);
      }
    } else {
      if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
        try {
          Field f = process.getClass().getDeclaredField("pid");
          f.setAccessible(true);
          return f.getInt(process);
        } catch (Throwable ignored) {
        }
      }
    }
    return -1;
  }

  private static boolean isJnaAvailable() {
    try {
      AnyProcess.class.getClassLoader().loadClass("com.sun.jna.platform.win32.Kernel32");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  protected static boolean isWindows() {
    return System.getProperty("os.name", "unknown").toLowerCase().contains("windows");
  }

  protected final String getCurrentPid() {
    try {
      return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    } catch (Exception ignored) {
      return null;
    }
  }

}
