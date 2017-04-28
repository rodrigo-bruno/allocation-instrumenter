/*
 * Copyright 2017 Google, Inc..
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
package com.google.monitoring.runtime.instrumentation;

import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author rbruno
 */
public class AllocStatisticsWritter {

    private static final String OUTPUT_ALLOCS = AllocationRecorder.OUTPUT_DIR + "/allocs/";
    private static final String OUTPUT_TRACES = AllocationRecorder.OUTPUT_DIR + "/traces/";

    static {
        new File(OUTPUT_ALLOCS).mkdirs();
        new File(OUTPUT_TRACES).mkdirs();
    }

    private static String expandStrackTrace(StackTraceElement[] st) {
        String res = "";
        // TODO - do this in another way to avoid object creation!
        for (int i = 0; i < st.length; i++) {
            res = res + "\n\t" + st[i];
        }
        return res;
    }

    public static void writeStatistics(int st_hash, ByteBuffer buf) throws Exception {
      FileChannel channel = new FileOutputStream(OUTPUT_ALLOCS + st_hash, true).getChannel();

      // Flips this buffer.  The limit is set to the current position and then
      // the position is set to zero.  If the mark is defined then it is discarded.
      buf.flip();

      int bytes = channel.write(buf);
      channel.close();

      buf.clear();

      if (AllocationRecorder.DEBUG || AllocationRecorder.DEBUG_STATS) {
          AllocationRecorder.LOG(String.format("Wrote %d bytes in allocs %s", bytes, st_hash));
      }
  }

    public static void writeStatistics(
      ConcurrentHashMap<Integer, ByteBuffer> allocs,
      ConcurrentHashMap<Integer, StackTraceElement[]> traces) throws Exception {
        // Write allocation logs.
        for (int st_hash : AllocationRecorder.allocs.keySet()) {
            writeStatistics(st_hash, allocs.get(st_hash));
        }

        // Write stacktrace logs.
        for (int st_hash : traces.keySet()) {
            if (AllocationRecorder.DEBUG || AllocationRecorder.DEBUG_STATS) {
                AllocationRecorder.LOG("Writing traces in traces" + st_hash);
            }
            FileOutputStream out = new FileOutputStream(OUTPUT_TRACES + st_hash, true);
            out.write(expandStrackTrace(traces.get(st_hash)).getBytes());
            out.close();
        }
    }
}
