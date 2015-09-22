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

import java.io.UnsupportedEncodingException;

import static org.fusesource.lmdbjni.JNI.*;

/**
 * Mostly flags and options used throughout the API.
 */
public class Constants {
  //====================================================//
  // Environment Flags
  //====================================================//

  /** mmap at a fixed address (experimental) */
  public static final int FIXEDMAP = MDB_FIXEDMAP;

  /** no environment directory */
  public static final int NOSUBDIR = MDB_NOSUBDIR;

  /** don't fsync after commit */
  public static final int NOSYNC = MDB_NOSYNC;

  /** read only */
  public static final int RDONLY = MDB_RDONLY;

  /** don't fsync metapage after commit */
  public static final int NOMETASYNC = MDB_NOMETASYNC;

  /** use writable mmap */
  public static final int WRITEMAP = MDB_WRITEMAP;

  /** use asynchronous msync when #MDB_WRITEMAP is used */
  public static final int MAPASYNC = MDB_MAPASYNC;
  /**
   * tie reader locktable slots to #MDB_txn objects
   * instead of to threads
   */
  public static final int NOTLS = MDB_NOTLS;

  /** don't do any locking, caller must manage their own locks */
  public static final int NOLOCK = MDB_NOLOCK;

  /** don't do readahead (no effect on Windows) */
  public static final int NORDAHEAD = MDB_NORDAHEAD;

  /** don't initialize malloc'd memory before writing to datafile */
  public static final int NOMEMINIT = MDB_NOMEMINIT;

  //====================================================//
  // Database Flags
  //====================================================//

  /** use reverse string keys */
  public static final int REVERSEKEY = MDB_REVERSEKEY;

  /** use sorted duplicates */
  public static final int DUPSORT = MDB_DUPSORT;

  /**
   * numeric keys in native byte order.
   * The keys must all be of the same size.
   */
  public static final int INTEGERKEY = MDB_INTEGERKEY;

  /**
   * with {@link org.fusesource.lmdbjni.Constants#DUPSORT},
   * sorted dup items have fixed size
   */
  public static final int DUPFIXED = MDB_DUPFIXED;

  /** with {@link org.fusesource.lmdbjni.Constants#DUPSORT}, use reverse string dups */
  public static final int INTEGERDUP = MDB_INTEGERDUP;

  /** create DB if not already existing */
  public static final int REVERSEDUP = MDB_REVERSEDUP;

  /** create DB if not already existing */
  public static final int CREATE = MDB_CREATE;

  //====================================================//
  // Write Flags
  //====================================================//

  /** For put: Don't write if the key already exists. */
  public static final int NOOVERWRITE = MDB_NOOVERWRITE;

  /**
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT} <br>
   * For put: don't write if the key and data pair already exist.<br>
   * For mdb_cursor_del: remove all duplicate data items.
   */
  public static final int NODUPDATA = MDB_NODUPDATA;

  /** For mdb_cursor_put: overwrite the current key/data pair */
  public static final int CURRENT = MDB_CURRENT;

  /**
   * For put: Just reserve space for data, don't copy it. Return a
   * pointer to the reserved space.
   */
  public static final int RESERVE = MDB_RESERVE;

  /** Data is being appended, don't split full pages. */
  public static final int APPEND = MDB_APPEND;

  /** Duplicate data is being appended, don't split full pages. */
  public static final int APPENDDUP = MDB_APPENDDUP;

  /**
   * Store multiple data items in one call. Only for
   * {@link org.fusesource.lmdbjni.Constants#DUPFIXED}.
   */
  public static final int MULTIPLE = MDB_MULTIPLE;

  //====================================================//
  // Cursor operations.
  //====================================================//

  /** Position at first key/data item */
  public static final GetOp FIRST = GetOp.FIRST;

  /**
   * Position at first data item of current key.
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp FIRST_DUP = GetOp.FIRST_DUP;

  /**
   * Position at key/data pair. Only for
   * {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp GET_BOTH = GetOp.GET_BOTH;

  /**
   * position at key, nearest data. Only for
   * {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp GET_BOTH_RANGE = GetOp.GET_BOTH_RANGE;

  /** Return key/data at current cursor position */
  public static final GetOp GET_CURRENT = GetOp.GET_CURRENT;

  /**
   * Return key and up to a page of duplicate data items
   * from current cursor position. Move cursor to prepare
   * for {@link org.fusesource.lmdbjni.Constants#NEXT_MULTIPLE}.
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp GET_MULTIPLE = GetOp.GET_MULTIPLE;

  /** Position at last key/data item */
  public static final GetOp LAST = GetOp.LAST;

  /**
   * Position at last data item of current key.
   * Only for{@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp LAST_DUP = GetOp.LAST_DUP;

  /** Position at next data item */
  public static final GetOp NEXT = GetOp.NEXT;

  /**
   * Position at next data item of current key.
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp NEXT_DUP = GetOp.NEXT_DUP;

  /**
   * Return key and up to a page of duplicate data items
   * from next cursor position. Move cursor to prepare
   * for {@link org.fusesource.lmdbjni.Constants#NEXT_MULTIPLE}.
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp NEXT_MULTIPLE = GetOp.NEXT_MULTIPLE;

  /** Position at first data item of next key */
  public static final GetOp NEXT_NODUP = GetOp.NEXT_NODUP;

  /** Position at previous data item */
  public static final GetOp PREV = GetOp.PREV;

  /**
   * Position at previous data item of current key.
   * Only for {@link org.fusesource.lmdbjni.Constants#DUPSORT}
   */
  public static final GetOp PREV_DUP = GetOp.PREV_DUP;

  /** Position at last data item of previous key */
  public static final GetOp PREV_NODUP = GetOp.PREV_NODUP;

  /** Position at specified key, return key + data */
  public static final SeekOp KEY = SeekOp.KEY;

  /** Position at first key greater than or equal to specified key. */
  public static final SeekOp RANGE = SeekOp.RANGE;

  private static final String UTF8_CHARSET = "UTF-8";

  public static byte[] bytes(String value) {
    if (value == null) {
      return null;
    }
    try {
      return value.getBytes(UTF8_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException ("Error getting byte string", e);
    }
  }

  public static String string(byte value[]) {
    if (value == null) {
      return null;
    }
    try {
      return new String(value, UTF8_CHARSET);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException ("Error getting string", e);
    }
  }
}
