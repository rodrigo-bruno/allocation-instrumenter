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

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class AllocationWorker extends Thread {

    private ByteBuffer allocateBuffer(int size, int st_hash) {
        if (AllocationRecorder.DEBUG || AllocationRecorder.DEBUG_WORKER) {
            AllocationRecorder.LOG("Allocating buffer with " + size + " bytes for " + st_hash);
        }
        return AllocationRecorder.OFFHEAP ?
                ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    @Override
    public void run() {
        // Objects created within this thread should not be recorded!
        com.google.monitoring.runtime.instrumentation.AllocationRecorder.recordingAllocation.set(Boolean.TRUE);
        AllocationRecord ar = null;
        while (true) {
            try {
                while (true) {
                    ar = AllocationRecorder.queue.poll(1, TimeUnit.DAYS);
                    if (ar != null) {
                        break;
                    }
                }
                StackTraceElement[] st = ar.getStackTrace();
                int obj_hash = ar.getObjHash();
                int st_hash = ar.getTraceHash();

                if (!AllocationRecorder.traces.contains(st_hash)) {
                    AllocationRecorder.traces.put(st_hash, st);
                }
                // TODO - there are some concurrency problems here!
                ByteBuffer bb = AllocationRecorder.allocs.get(st_hash);

                // If this is the first time with this stack trace hash.
                if (bb == null) {
                    bb = allocateBuffer(AllocationRecorder.BUFF_SIZE, st_hash);
                    AllocationRecorder.allocs.put(st_hash, bb);
                }

                // If the buffer is full.
                if (bb.remaining() == 0) {
                    AllocStatisticsWritter.writeStatistics(st_hash, bb);
                    // Increase the buffer size if not alredy at max.
                    if (bb.array().length < AllocationRecorder.BUFF_SIZE_MAX) {
                        bb = allocateBuffer(bb.array().length*2, st_hash);
                        AllocationRecorder.allocs.put(st_hash, bb);
                    }
                }

                bb.putInt(obj_hash);
                if (AllocationRecorder.DEBUG || AllocationRecorder.DEBUG_WORKER) {
                    AllocationRecorder.LOG("Inserted object " + obj_hash + " into " + st_hash);
                }
            }
            catch (InterruptedException ie) {
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
