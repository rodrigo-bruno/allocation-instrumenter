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

public class AllocationRecord extends Throwable {
    private final int objHash;
    private boolean isSTHashed;
    private int stHash;

    public AllocationRecord(int objHash) {
        super();
        this.objHash = objHash;
        isSTHashed = false;
    }

    private int hashStackTrace(StackTraceElement[] st) {
        int result = 37;
        try {
          for (StackTraceElement ste : st) {
              result = 37*result + ste.hashCode();
          }
        } catch (Exception e) {
          if (AllocationRecorder.DEBUG || AllocationRecorder.DEBUG_WARNS) {
            AllocationRecorder.LOG("ERR: hash stack trace:");
            e.printStackTrace();
          }
        }
        return result;
    }

    public int getObjHash() {
        return this.objHash;
    }

    public int getTraceHash() {
        if (!isSTHashed) {
            this.stHash = hashStackTrace(super.getStackTrace());
            isSTHashed = true;
        }
        return this.stHash;
    }
}
