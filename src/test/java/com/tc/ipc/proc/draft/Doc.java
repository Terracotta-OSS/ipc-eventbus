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

package com.tc.ipc.proc.draft;

import com.tc.ipc.event.Event;
import com.tc.ipc.event.EventBus;
import com.tc.ipc.event.EventBusClient;
import com.tc.ipc.event.EventBusServer;
import com.tc.ipc.event.EventListener;
import com.tc.ipc.event.EventListenerSniffer;
import com.tc.ipc.event.PrintingErrorListener;
import com.tc.ipc.event.RethrowingErrorListener;
import com.tc.ipc.io.MultiplexOutputStream;
import com.tc.ipc.io.Pipe;
import com.tc.ipc.proc.AnyProcess;
import com.tc.ipc.proc.Echo;
import com.tc.ipc.proc.EventJavaProcess;
import com.tc.ipc.proc.JavaProcess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class Doc {
  public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    OutputStream outputStream2 = null;
    byte[] data = null;
    Event event = null;

    Pipe pipe = new Pipe("collect stdout", inputStream, outputStream);
    pipe.waitFor();     // if you need, you can wait for the pipe to finish
    pipe.close();       // close the pipe (does not close the stream!)

    MultiplexOutputStream plex = new MultiplexOutputStream()
        .addOutputStream(outputStream)
        .addOutputStream(outputStream2)
        .addOutputStream(System.out);

    plex.getOutputStreams();    //  lists the streams
    plex.isEmpty();             // true if no streams
    plex.streamCount();         // number of streams
    plex.write(data);           // MultiplexOutputStream is an OutputStream

    EventBus eventBus = new EventBus.Builder()
        .onError(new PrintingErrorListener(System.err)) // OPTIONAL: print listener exceptions
        .onError(new RethrowingErrorListener())         // OPTIONAL: rethrow listener exceptions immediately (by default)
        .id("bus-id")                                   // OPTIONAL: bus id (otherwise a UUID is generated)
        .build();

    eventBus.getId(); // returns the eventbus id

    EventListener listener = new EventListener() {
      @Override
      public void onEvent(Event e) {
      }
    };

    eventBus.on(listener);              // register a listener for all events
    eventBus.on("my.event", listener);  // register a listener for a specific event

    eventBus.on(new EventListenerSniffer()); // listener which dumps to the console all events (for debug)

    eventBus.unbind(listener);              // removes a listener from all events
    eventBus.unbind("my.event");            // removes all listeners for this event
    eventBus.unbind("my.event", listener);  // removes a specific listener from an event

    eventBus.trigger("my.event");               // can trigger an event
    eventBus.trigger("my.event", "some data");  // event with data (must be serializable)

    // Listener will receive an event:
    event.getData();                // the data
    event.getData(String.class);    // can cast the data in the wanted type
    event.getName();                // the name of the event triggered
    event.getSource();              // the ID of the eventbus
    event.getTimestamp();           // time in ms of the event
    event.isUserEvent();            // check if it is a user event. You might listen to system events such as eventbus.server.close, eventbus.client.connect, eventbus.client.disconnect

    EventBusServer server = new EventBusServer.Builder()
        .id("peer1")     // OPTIONAL: bus id
        .bind("0.0.0.0") // OPTIONAL: bind address
        .listen(56789)   // OPTIONAL: port to listen to. Default to 56789
        .listenRandom()  // OPTIONAL: choose a random port for listening
        .build();

    EventBusClient client = new EventBusClient.Builder()
        .id("peer2")
        .connect(56789)              // OPTIONAL: port to connect to
        .connect("lcoalhost", 56789) // OPTIONAL: port and host to connect to. Default is localhost:56789
        .build();

    AnyProcess process = AnyProcess.newBuilder()
        .command("bash", "-c", "sleep 3; echo $VAR")
        .recordStdout()                    // OPTIONAL: save stdout from process for getStdout() (disabled by default). Disables getInputStream().
        .recordStderr()                    // OPTIONAL: save stderr from process for getStderr() (disabled by default). Disabled getErrorStream().
        .env("key", "value")                // OPTIONAL: add a env. variable
        .env(new HashMap<String, String>()) // OPTIONAL: se ta new env
        .pipeStderr()                       // OPTIONAL: send stderr to the console
        .pipeStderr(outputStream)           // OPTIONAL: send stderr to a stream. You can both collect and pipe.
        .pipeStdout()                       // OPTIONAL: send stdout to the console
        .pipeStdout(outputStream)           // OPTIONAL: send stdout to a stream. You can both collect and pipe.
        .pipeStdin()                        // OPTIONAL: will bind process stdin to this process stding
        .pipeStdin(inputStream)             // OPTIONAL: will bind process stdin to this input stream
        .redirectStderr()                   // OPTIONAL: treat stderr like stdout (both merged into stdout)
        .workingDir(new File("."))          // OPTIONAL: change the working directly. Same as current process by default
        .build();

    process.destroy();                          // destroy (kill with SIGTERM) the process
    process.exitValue();                        // the process exit value, when available
    process.getCommand();                       // the process command
    process.getErrorStream();
    process.getFuture();                        // get a future representing the process execution. You can cancel (=destroy) the process or wait for its completion
    process.getInputStream();
    process.getOutputStream();
    process.getPid();                           // get the process PID
    process.getRecordedStderr();                        // if collected, get the stderr of the process
    process.getRecordedStderrText();                    // if collected, get the stderr of the process as a String
    process.getRecordedStdout();                        // if collected, get the stdout of the process
    process.getRecordedStdoutText();                    // if collected, get the stdout of the process as a String
    process.getWorkingDirectory();
    process.isDestroyed();                      // check if process is destroyed
    process.isRunning();                        // check if process is still running
    process.waitFor();                          // wait and block while process finished
    process.waitForTime(1, TimeUnit.MINUTES);   // wait for the process to finish or timeout

    JavaProcess javaProcess = JavaProcess.newBuilder()
        .mainClass("my.company.Echo")
        .addClasspath(Echo.class)           // add a classpath entry from a class (find the enclosing jar or folder)
        .arguments("one", "two")            // add some program arguments
        .addJvmProp("my.prop", "world")     // add some jvm props
        .addJvmArg("-Xmx512m")              // add some jvm flags
        .env("VAR", "Hello")                // add some env. variable
        .pipeStdout()                       // you can access all process builder methods seen above
        .pipeStderr()
        .recordStdout()
        .recordStderr()
        .build();

    javaProcess.getJavaExecutable();            // automatically resolved from java home, but you can override it in the builder
    javaProcess.getJavaHome();                  // automatically resolved from java home, but you can override it in the builder

    EventJavaProcess ipc = EventJavaProcess.newBuilder()
        .port(1234)
        .mainClass("my.corp.Echo")
        .arguments("one", "two")
        .build();

  }

}
