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

import java.io.Closeable;
import java.nio.ByteBuffer;

import static org.fusesource.lmdbjni.JNI.*;
import static org.fusesource.lmdbjni.Util.checkArgNotNull;
import static org.fusesource.lmdbjni.Util.checkErrorCode;

/**
 * A cursor handle.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Cursor extends NativeObject implements Closeable {
  DirectBuffer buffer;
  long bufferAddress;
  final boolean isReadOnly;

  Cursor(long self, boolean isReadOnly) {
    super(self);
    this.isReadOnly = isReadOnly;
  }

  private void initBuffer () {
    this.buffer = new DirectBuffer(ByteBuffer.allocateDirect(Unsafe.ADDRESS_SIZE * 4));
    this.bufferAddress = buffer.addressOffset();
  }

  /**
   * <p>
   *   Close a cursor handle.
   * </p>
   *
   * The cursor handle will be freed and must not be used again after this call.
   * Its transaction must still be live if it is a write-transaction.
   */
  @Override
  public void close() {
    if (self != 0) {
      mdb_cursor_close(self);
      self = 0;
    }
  }

  /**
   * <p>
   *   Renew a cursor handle.
   * </p>
   *
   * A cursor is associated with a specific transaction and database.
   * Cursors that are only used in read-only
   * transactions may be re-used, to avoid unnecessary malloc/free overhead.
   * The cursor may be associated with a new read-only transaction, and
   * referencing the same database handle as it was created with.
   * This may be done whether the previous transaction is live or dead.
   *
   * @param tx transaction handle
   */
  public void renew(Transaction tx) {
    checkErrorCode(mdb_cursor_renew(tx.pointer(), pointer()));
  }

  /**
   * <p>
   *   Retrieve by cursor.
   * </p>
   * This function retrieves key/data pairs from the database. The address and length
   * of the key are returned in the object to which \b key refers (except for the
   * case of the #MDB_SET option, in which the \b key object is unchanged), and
   * the address and length of the data are returned in the object to which \b data
   *
   * @param op A cursor operation #MDB_cursor_op
   * @return
   */
  public Entry get(GetOp op) {
    checkArgNotNull(op, "op");

    Value key = new Value();
    Value value = new Value();
    int rc = mdb_cursor_get(pointer(), key, value, op.getValue());
    if (rc == MDB_NOTFOUND) {
      return null;
    }
    checkErrorCode(rc);
    return new Entry(key.toByteArray(), value.toByteArray());
  }

  /**
   * @see org.fusesource.lmdbjni.Cursor#get(GetOp)
   */
  public int position(DirectBuffer key, DirectBuffer value, GetOp op) {
    checkArgNotNull(op, "op");
    if (buffer == null) initBuffer();
    int rc = mdb_cursor_get_address(pointer(), bufferAddress, bufferAddress + 2 * Unsafe.ADDRESS_SIZE, op.getValue());
    if (rc == MDB_NOTFOUND) {
      return rc;
    }
    checkErrorCode(rc);
    wrapBufferAddress(key, value);
    return rc;
  }

  /**
   * Same as get but with a seek operation.
   * @see org.fusesource.lmdbjni.Cursor#get(GetOp)
   */
  public int seekPosition(DirectBuffer key, DirectBuffer value, SeekOp op) {
    checkArgNotNull(key, "key");
    checkArgNotNull(value, "value");
    checkArgNotNull(op, "op");
    if (buffer == null) initBuffer();
    Unsafe.putLong(bufferAddress, 0, key.capacity());
    Unsafe.putLong(bufferAddress, 1, key.addressOffset());

    int rc = mdb_cursor_get_address(pointer(), bufferAddress, bufferAddress + 2 * Unsafe.ADDRESS_SIZE, op.getValue());
    if (rc == MDB_NOTFOUND) {
      return rc;
    }
    checkErrorCode(rc);
    wrapBufferAddress(key, value);
    return rc;
  }

  private void wrapBufferAddress(DirectBuffer key, DirectBuffer value) {
    int keySize = (int) Unsafe.getLong(bufferAddress, 0);
    key.wrap(Unsafe.getAddress(bufferAddress, 1), keySize);
    int valSize = (int) Unsafe.getLong(bufferAddress, 2);
    value.wrap(Unsafe.getAddress(bufferAddress, 3), valSize);
  }

  /**
   * Same as get but with a seek operation.
   * For GetOp.KEY and GetOp.RANGE only
   *
   * @see org.fusesource.lmdbjni.Cursor#get(GetOp)
   */
  public Entry seek(SeekOp op, byte[] key) {
    checkArgNotNull(key, "key");
    checkArgNotNull(op, "op");
    NativeBuffer keyBuffer = NativeBuffer.create(key);
    try {
      Value keyValue = new Value(keyBuffer);
      Value value = new Value();
      int rc = mdb_cursor_get(pointer(), keyValue, value, op.getValue());
      if (rc == MDB_NOTFOUND) {
        return null;
      }
      checkErrorCode(rc);
      return new Entry(keyValue.toByteArray(), value.toByteArray());
    } finally {
      keyBuffer.delete();
    }

  }

  /**
   * Same as get and seek but pass in the value in addition to the key.
   * For GetOp.BOTH and GetOp.BOTH_RANGE only
   *
   * @see org.fusesource.lmdbjni.Cursor#get(GetOp)
   */
  public Entry seek(SeekOp op, byte[] key, byte[] value) {
    checkArgNotNull(key, "key");
    checkArgNotNull(value, "value");
    checkArgNotNull(op, "op");
    NativeBuffer keyBuffer = NativeBuffer.create(key);
    NativeBuffer valueBuffer = NativeBuffer.create(value);
    try {
      Value keyValue = new Value(keyBuffer);
      Value valueVal = new Value(valueBuffer);
      int rc = mdb_cursor_get(pointer(), keyValue, valueVal, op.getValue());
      if (rc == MDB_NOTFOUND) {
        return null;
      }
      checkErrorCode(rc);
      return new Entry(keyValue.toByteArray(), valueVal.toByteArray());
    } finally {
      keyBuffer.delete();
      valueBuffer.delete();
    }

  }

  /**
   * <p>
   *   Store by cursor.
   * </p>
   *
   * @param key The key operated on.
   * @param value The data operated on.
   * @param flags Options for this operation. This parameter
   * must be set to 0 or one of the values described here.
   * <ul>
   *	<li>{@link org.fusesource.lmdbjni.Constants#CURRENT} -
   *    replace the item at the current cursor position.
   *		The \b key parameter must still be provided, and must match it.
   *		If using sorted duplicates ({@link org.fusesource.lmdbjni.Constants#DUPSORT})
   *	  the data item must still
   *		sort into the same place. This is intended to be used when the
   *		new data is the same size as the old. Otherwise it will simply
   *		perform a delete of the old record followed by an insert.
   *	<li>{@link org.fusesource.lmdbjni.Constants#NODUPDATA} -
   *    enter the new key/data pair only if it does not
   *		already appear in the database. This flag may only be specified
   *		if the database was opened with {@link org.fusesource.lmdbjni.Constants#DUPSORT}.
   *	  The function will return {@link org.fusesource.lmdbjni.LMDBException#KEYEXIST}
   *	  if the key/data pair already appears in the database.
   *	<li>{@link org.fusesource.lmdbjni.Constants#NOOVERWRITE} -
   *    enter the new key/data pair only if the key does not already appear
   *    in the database. The function will return {@link org.fusesource.lmdbjni.LMDBException#KEYEXIST}
   *    if the key already appears in the database, even if the database supports duplicates
   *    ({@link org.fusesource.lmdbjni.LMDBException#KEYEXIST}).
   *	<li>{@link org.fusesource.lmdbjni.Constants#RESERVE} - reserve space
   *    for data of the given size, but don't copy the given data.
   *    Instead, return a pointer to the reserved space, which the caller
   *    can fill in later. This saves an extra memcpy if the data is being
   *    generated later.
   *	<li>{@link org.fusesource.lmdbjni.Constants#APPEND} - append the given
   *    key/data pair to the end of the database. No key comparisons are
   *    performed. This option allows fast bulk loading when keys are already
   *    known to be in the correct order. Loading unsorted keys with this flag
   *    will cause data corruption.
   *	<li>{@link org.fusesource.lmdbjni.Constants#APPENDDUP} - as above, but for
   *    sorted dup data.
   *	<li>{@link org.fusesource.lmdbjni.Constants#MULTIPLE} - store multiple
   *    contiguous data elements in a single request. This flag may only be
   *    specified if the database was opened with {@link org.fusesource.lmdbjni.Constants#DUPFIXED}.
   *    The \b data argument must be an array of two MDB_vals. The mv_size
   *    of the first MDB_val must be the size of a single data element. The mv_data
   *    of the first MDB_val must point to the beginning of the array of contiguous
   *    data elements. The mv_size of the second MDB_val must be the count of the number
   *		of data elements to store. On return this field will be set to
   *		the count of the number of elements actually written. The mv_data
   *		of the second MDB_val is unused.
   * </ul>
   * @return the value that was stored
   */
  public byte[] put(byte[] key, byte[] value, int flags) {
    checkArgNotNull(key, "key");
    checkArgNotNull(value, "value");
    NativeBuffer keyBuffer = NativeBuffer.create(key);
    try {
      NativeBuffer valueBuffer = NativeBuffer.create(value);
      try {
        return put(keyBuffer, valueBuffer, flags);
      } finally {
        valueBuffer.delete();
      }
    } finally {
      keyBuffer.delete();
    }
  }

  /**
   * @see org.fusesource.lmdbjni.Cursor#put(byte[], byte[], int)
   */
  public int put(DirectBuffer key, DirectBuffer value, int flags) {
    checkArgNotNull(key, "key");
    checkArgNotNull(value, "value");
    if (buffer == null) initBuffer();
    Unsafe.putLong(bufferAddress, 0, key.capacity());
    Unsafe.putLong(bufferAddress, 1, key.addressOffset());
    Unsafe.putLong(bufferAddress, 2, value.capacity());
    Unsafe.putLong(bufferAddress, 3, value.addressOffset());
    return mdb_cursor_put_address(pointer(), bufferAddress, bufferAddress + 2 * Unsafe.ADDRESS_SIZE, flags);
  }

  public byte[] put(NativeBuffer keyBuffer, NativeBuffer valueBuffer, int flags) {
    return put(new Value(keyBuffer), new Value(valueBuffer), flags);
  }

  public byte[] put(NativeBuffer keyBuffer, int keyLength, NativeBuffer valueBuffer, int valueLength, int flags) {
    return put(new Value(keyBuffer, keyLength), new Value(valueBuffer, valueLength), flags);
  }

  private byte[] put(Value keySlice, Value valueSlice, int flags) {
    mdb_cursor_put(pointer(), keySlice, valueSlice, flags);
    return valueSlice.toByteArray();
  }


  /**
   * Just reserve space for data in the database, don't copy it.
   *
   * @return a pointer to the reserved   space.
   */
  public DirectBuffer reserve(DirectBuffer key, int size) {
    checkArgNotNull(key, "key");
    if (key.byteArray() != null || !key.byteBuffer().isDirect()) {
      throw new IllegalArgumentException("Key buffer is not direct.");
    }
    if (buffer == null) {
      buffer = new DirectBuffer(ByteBuffer.allocateDirect(Unsafe.ADDRESS_SIZE * 4));
      bufferAddress = buffer.addressOffset();
    }
    Unsafe.putLong(bufferAddress, 0, key.capacity());
    Unsafe.putLong(bufferAddress, 1, key.addressOffset());
    Unsafe.putLong(bufferAddress, 2, size);
    int rc = mdb_cursor_put_address(pointer(), bufferAddress, bufferAddress + 2 * Unsafe.ADDRESS_SIZE, Constants.RESERVE);
    checkErrorCode(rc);
    int valSize = (int) Unsafe.getLong(bufferAddress, 2);
    long valAddress = Unsafe.getAddress(bufferAddress, 3);
    DirectBuffer empty = new DirectBuffer(0, 0);
    empty.wrap(valAddress, valSize);
    return empty;
  }

  /**
   * <p>
   *   Delete current key/data pair.
   * </p>
   *
   * This function deletes the key/data pair to which the cursor refers.
   */
  public void delete() {
    checkErrorCode(mdb_cursor_del(pointer(), 0));
  }
  /**
   * <p>
   *   Delete current key/data pair.
   * </p>
   *
   * This function deletes all of the data items for the current key.
   *
   * May only be called if the database was opened with
   * {@link org.fusesource.lmdbjni.Constants#DUPSORT}.
   */
  public void deleteIncludingDups() {
    checkErrorCode(mdb_cursor_del(pointer(), MDB_NODUPDATA));
  }

  /**
   * <p>
   *  Return count of duplicates for current key.
   * </p>
   *
   * This call is only valid on databases that support sorted duplicate
   * data items {@link org.fusesource.lmdbjni.Constants#DUPSORT}.
   *
   * @return count of duplicates for current key
   */
  public long count() {
    long rc[] = new long[1];
    checkErrorCode(mdb_cursor_count(pointer(), rc));
    return rc[0];
  }

  public boolean isReadOnly() {
    return isReadOnly;
  }
}
