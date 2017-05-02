/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.monitoring.runtime.instrumentation;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.LinkedList;

/**
 * The logic for recording allocations, called from bytecode rewritten by
 * {@link AllocationInstrumenter}.
 *
 * @author jeremymanson@google.com (Jeremy Manson)
 * @author fischman@google.com (Ami Fischman)
 */
public class AllocationRecorder {
  static {
    // Sun's JVMs in 1.5.0_06 and 1.6.0{,_01} have a bug where calling
    // Instrumentation.getObjectSize() during JVM shutdown triggers a
    // JVM-crashing assert in JPLISAgent.c, so we make sure to not call it after
    // shutdown.  There can still be a race here, depending on the extent of the
    // JVM bug, but this seems to be good enough.
    // instrumentation is volatile to make sure the threads reading it (in
    // recordAllocation()) see the updated value; we could do more
    // synchronization but it's not clear that it'd be worth it, given the
    // ambiguity of the bug we're working around in the first place.
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG("running shutdown hook...");
        setInstrumentation(null);
        closeOutputSteams();
        LOG("running shutdown hook...Done");
      }
    });
  }

  // Debug flags
  protected static final boolean DEBUG = true;
  protected static final boolean DEBUG_ALLOCS = true;
  protected static final boolean DEBUG_WARNS = true;

  // Where alloc statistics are going to be placed.
  protected static String OUTPUT_DIR = "/tmp";

  // See the comment above the addShutdownHook in the static block above
  // for why this is volatile.
  private static volatile Instrumentation instrumentation = null;

  static Instrumentation getInstrumentation() {
    return instrumentation;
  }

  static void setInstrumentation(Instrumentation inst) {
    instrumentation = inst;
  }

  // Used for reentrancy checks
  public static final ThreadLocal<Boolean> recordingAllocation = new ThreadLocal<Boolean>();

  // Each thread contains an object stream.
  public static final ThreadLocal<ObjectOutputStream> outputStream = new ThreadLocal<ObjectOutputStream>();

  public static final List<ObjectOutputStream> outputStreams;

  static {
    outputStreams = new LinkedList<ObjectOutputStream>();
  }

  public static void addOutputStream(ObjectOutputStream oos) {
    synchronized (outputStreams) {
      outputStreams.add(oos);
    }
  }

  public static void closeOutputSteams() {
    synchronized (outputStreams) {
      for (ObjectOutputStream oos : outputStreams) {
        try {
          oos.flush();
          oos.close();
        }
        catch (Exception e) {
          if (DEBUG || DEBUG_WARNS) {
            LOG("ERR: unable to close output stream");
            e.printStackTrace();
          }
        }
      }
    }
  }
  public static synchronized void LOG(String msg) {
    System.err.println("[olr-ar] " + msg);
  }

  public static void recordAllocation(Class<?> cls, Object newObj) {
    // The use of replace makes calls to this method relatively ridiculously
    // expensive.
    String typename = cls.getName().replace('.', '/');
    recordAllocation(-1, typename, newObj);
  }

  /**
   * Records the allocation.  This method is invoked on every allocation
   * performed by the system.
   *
   * @param count the count of how many instances are being
   *   allocated, if an array is being allocated.  If an array is not being
   *   allocated, then this value will be -1.
   * @param desc the descriptor of the class/primitive type
   *   being allocated.
   * @param newObj the new <code>Object</code> whose allocation is being
   *   recorded.
   */
  public static void recordAllocation(int count, String desc, Object newObj) {
    if (recordingAllocation.get() == Boolean.TRUE) {
      return;
    } else {
      recordingAllocation.set(Boolean.TRUE);
    }

    // Copy value into local variable to prevent NPE that occurs when
    // instrumentation field is set to null by this class's shutdown hook
    // after another thread passed the null check but has yet to call
    // instrumentation.getObjectSize()
    Instrumentation instr = instrumentation;
    if (instr != null) {
        // TODO - check if we can strip down the amount of serialized data.
      AllocationRecord ar = new AllocationRecord(System.identityHashCode(newObj));
      ObjectOutputStream oos = outputStream.get();
      try {
        if (oos == null) {
          String output = OUTPUT_DIR + "/olr-ar-" + Thread.currentThread().getId();
          oos = new ObjectOutputStream(
                  // TODO - intruduce buffered output steam in between.
                  new FileOutputStream(output, true));
          outputStream.set(oos);
          addOutputStream(oos);
          if (DEBUG || DEBUG_ALLOCS) {
            LOG("new output steam in " + output);
          }
        }
        oos.writeObject(ar);
      } catch (Exception e) {
        if (DEBUG || DEBUG_WARNS) {
          LOG(String.format("ERR: unable to write to output stream for thread %d",
           Thread.currentThread().getId()));
          e.printStackTrace();
        }
      }

      if (DEBUG || DEBUG_ALLOCS) {
        LOG(String.format("st=%d\tobj=%d\tcount=%d\tdesc=%sin=%s",
           ar.getTraceHash(), ar.getObjHash(), count, desc, OUTPUT_DIR + "/olr-ar-" + Thread.currentThread().getId()));
      }
    }

    recordingAllocation.set(Boolean.FALSE);
  }

  /**
   * Helper method to force recording; for unit tests only.
   * @param count the number of objects being allocated.
   * @param desc the descriptor of the class of the object being allocated.
   * @param newObj the object being allocated.
   */
  public static void recordAllocationForceForTest(int count, String desc,
                                                  Object newObj) {
    // Make sure we get the right number of elided frames
    recordAllocationForceForTestReal(count, desc, newObj, 2);
  }

  /**
   * Helper method to force recording; for unit tests only.
   * @param count the number of objects being allocated.
   * @param desc the descriptor of the class of the object being allocated.
   * @param newObj the object being allocated.
   * @param recurse A recursion count.
   */
  public static void recordAllocationForceForTestReal(
      int count, String desc, Object newObj, int recurse) {
    if (recurse != 0) {
      recordAllocationForceForTestReal(count, desc, newObj, recurse - 1);
      return;
    }
  }
}
