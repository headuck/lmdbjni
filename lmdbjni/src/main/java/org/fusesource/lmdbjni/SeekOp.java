/**
 * Copyright (C) 2013, RedHat, Inc.
 *
 *    http://www.redhat.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.lmdbjni;

import static org.fusesource.lmdbjni.JNI.*;

/**
 * Cursor Seek operations.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public enum SeekOp {
  /** Position at specified key, return key + data */
  KEY(MDB_SET_KEY),

  /** Position at first key greater than or equal to specified key. */
  RANGE(MDB_SET_RANGE),

  /** Position at key/data pair. Only for MDB_DUPSORT. */
  BOTH(MDB_GET_BOTH),

  /** Position at position at key, nearest data.  Only for MDB_DUPSORT. */
  BOTH_RANGE(MDB_GET_BOTH_RANGE);

  private final int value;

  SeekOp(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
