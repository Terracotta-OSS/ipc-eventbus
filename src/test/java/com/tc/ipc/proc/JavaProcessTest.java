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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class JavaProcessTest {

  @Test
  public void test_launch_java_process() throws InterruptedException {
    JavaProcess proc = JavaProcess.newBuilder()
        .mainClass(Echo.class.getName())
        .addClasspath(Echo.class)
        .addClasspath(JUnit4.class)
        .arguments("one", "two")
        .addJvmProp("my.prop", "world")
        .addJvmArg("-Xmx512m")
        .env("VAR", "Hello")
        .pipeStdout()
        .pipeStderr()
        .recordStdout()
        .recordStderr()
        .build();

    System.out.println(proc.getCommand());
    assertEquals(0, proc.waitFor());
    assertEquals("Hello\n" +
        "world\n" +
        "one\n" +
        "two\n", proc.getRecordedStdoutText());
    assertEquals("", proc.getRecordedStderrText());
  }

  @Test
  public void test_launch_failing_java_process() throws InterruptedException {
    JavaProcess proc = JavaProcess.newBuilder()
        .mainClass(EchoFail.class.getName())
        .addClasspath(EchoFail.class)
        .addClasspath(JUnit4.class)
        .arguments("one", "two")
        .addJvmProp("my.prop", "world")
        .addJvmArg("-Xmx512m")
        .env("VAR", "Hello")
        .recordStdout()
        .recordStderr()
        .build();

    System.out.println(proc.getCommand());
    assertEquals(1, proc.waitFor());
    assertEquals("Hello\n" +
        "world\n" +
        "one\n" +
        "two\n", proc.getRecordedStdoutText());
    assertTrue(proc.getRecordedStderrText().contains("Exception in thread \"main\" java.lang.AssertionError: message"));
  }

}
