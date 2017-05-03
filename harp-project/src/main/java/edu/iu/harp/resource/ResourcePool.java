/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.harp.resource;

public class ResourcePool {

  private static ResourcePool instance = null;

  private final BytesPool byteArrays;
  private final ShortsPool shortArrays;
  private final IntsPool intArrays;
  private final FloatsPool floatArrays;
  private final LongsPool longArrays;
  private final DoublesPool doubleArrays;
  private final WritablePool writables;

  private ResourcePool() {
    byteArrays = new BytesPool();
    shortArrays = new ShortsPool();
    intArrays = new IntsPool();
    floatArrays = new FloatsPool();
    longArrays = new LongsPool();
    doubleArrays = new DoublesPool();
    writables = new WritablePool();
  }

  public static ResourcePool get() {
    if (instance != null) {
      return instance;
    } else {
      return create();
    }
  }

  private static synchronized ResourcePool
    create() {
    if (instance == null) {
      instance = new ResourcePool();
    }
    return instance;
  }

  BytesPool getBytesPool() {
    return byteArrays;
  }

  ShortsPool getShortsPool() {
    return shortArrays;
  }

  IntsPool getIntsPool() {
    return intArrays;
  }

  FloatsPool getFloatsPool() {
    return floatArrays;
  }

  LongsPool getLongsPool() {
    return longArrays;
  }

  DoublesPool getDoublesPool() {
    return doubleArrays;
  }

  WritablePool getWritablePool() {
    return writables;
  }

  public void clean() {
    byteArrays.clean();
    shortArrays.clean();
    intArrays.clean();
    floatArrays.clean();
    longArrays.clean();
    doubleArrays.clean();
    writables.clean();
  }

  public void log() {
    byteArrays.log();
    shortArrays.log();
    intArrays.log();
    floatArrays.log();
    longArrays.log();
    doubleArrays.log();
    writables.log();
  }
}
