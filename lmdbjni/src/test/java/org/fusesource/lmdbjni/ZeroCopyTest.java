package org.fusesource.lmdbjni;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.lmdbjni.Constants.FIRST;
import static org.fusesource.lmdbjni.Constants.NEXT;
import static org.fusesource.lmdbjni.LMDBException.NOTFOUND;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ZeroCopyTest {
  static {
    Setup.setLmdbLibraryPath();
  }

  @Rule
  public TemporaryFolder dir = new TemporaryFolder();

  private Database db;
  private Env env;

  private DirectBuffer k1 = new DirectBuffer();
  private DirectBuffer v1 = new DirectBuffer();
  private DirectBuffer k2 = new DirectBuffer();
  private DirectBuffer v2 = new DirectBuffer();

  @Before
  public void before() throws IOException {
    String path = dir.newFolder().getCanonicalPath();
    if (db != null) {
      db.close();
    }
    if (env != null) {
      env.close();
    }
    env = new Env();
    env.open(path);
    db = env.openDatabase("test");
  }

  @Test
  public void testPutAndGetAndDelete() throws Exception {
    k1.putLong(0, 10);
    v1.putLong(0, 11);
    k2.putLong(0, 12);
    v2.putLong(0, 13);

    db.put(k1, v1);
    db.put(k2, v2);

    DirectBuffer k = new DirectBuffer();
    DirectBuffer v = new DirectBuffer();
    k.putLong(0, 10);
    db.get(k, v);
    assertThat(v.getLong(0), is(11L));

    db.delete(k);
    try {
      db.get(k, v);
    } catch (LMDBException e) {
      assertThat(e.errorCode, is(NOTFOUND));
    }
  }

  @Test
  public void testCursorPutAndGet() throws Exception {
    k1.putLong(0, 14);
    v1.putLong(0, 15);
    k2.putLong(0, 16);
    v2.putLong(0, 17);

    try (Transaction tx = env.createWriteTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        cursor.put(k1, v1, 0);
        cursor.put(k2, v2, 0);
      }
      tx.commit();
    }

    DirectBuffer k = new DirectBuffer();
    DirectBuffer v = new DirectBuffer();

    try (Transaction tx = env.createReadTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        cursor.position(k, v, GetOp.FIRST);
        assertThat(k.getLong(0), is(14L));
        assertThat(v.getLong(0), is(15L));

        cursor.position(k, v, GetOp.NEXT);
        assertThat(k.getLong(0), is(16L));
        assertThat(v.getLong(0), is(17L));
      }
    }
  }


  @Test
  public void testCursorSeekRange() throws Exception {
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
    k1.putLong(0, 18);
    v1.putLong(0, 19);
    k2.putLong(0, 20);
    v2.putLong(0, 21);

    try (Transaction tx = env.createWriteTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        cursor.put(k1, v1, 0);
        cursor.put(k2, v2, 0);
      }
      tx.commit();
    }

    try (Transaction tx = env.createReadTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        DirectBuffer k = new DirectBuffer(byteBuffer);
        DirectBuffer v = new DirectBuffer();
        k.putLong(0, 10);

        cursor.seekPosition(k, v, SeekOp.RANGE);
        assertThat(k.getLong(0), is(18L));
        assertThat(v.getLong(0), is(19L));

        k.wrap(byteBuffer);
        k.putLong(0, 19);
        cursor.seekPosition(k, v, SeekOp.RANGE);
        assertThat(k.getLong(0), is(20L));
        assertThat(v.getLong(0), is(21L));
      }
    }
  }

  @Test
  public void testIteration() {
    k1.putLong(0, 18);
    v1.putLong(0, 19);
    k2.putLong(0, 20);
    v2.putLong(0, 21);
    db.put(k1, v1, 0);
    db.put(k2, v2, 0);
    List<Long> result = new ArrayList<>();
    try (Transaction tx = env.createWriteTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        DirectBuffer k = new DirectBuffer();
        DirectBuffer v = new DirectBuffer();
        for (int rc = cursor.position(k, v, FIRST); rc != NOTFOUND; rc = cursor.position(k, v, NEXT)) {
          result.add(k.getLong(0));
        }
      }
      tx.commit();
    }
    assertThat(result.size(), is(2));
    assertThat(result.get(0), is(18L));
    assertThat(result.get(1), is(20L));
  }

  @Test
  public void testReserve() {
    byte[] key = new byte[]{1, 2, 3};
    byte[] val = new byte[]{3, 2, 1};

    try (Transaction tx = env.createWriteTransaction()) {
      DirectBuffer keyBuf = new DirectBuffer(ByteBuffer.allocateDirect(key.length));
      keyBuf.putBytes(0, key);
      DirectBuffer valBuf = db.reserve(tx, keyBuf, val.length);
      valBuf.putBytes(0, val);
      tx.commit();
    }

    try (Transaction tx = env.createReadTransaction()) {
      byte[] result = db.get(tx, key);
      assertArrayEquals(result, val);
    }
  }

  @Test
  public void testReserveCursor() {
    byte[] key = new byte[]{1, 1, 1};
    byte[] val = new byte[]{3, 3, 3};

    try (Transaction tx = env.createWriteTransaction()) {
      try (Cursor cursor = db.openCursor(tx)) {
        DirectBuffer keyBuf = new DirectBuffer(ByteBuffer.allocateDirect(key.length));
        keyBuf.putBytes(0, key);
        DirectBuffer valBuf = cursor.reserve(keyBuf, val.length);
        valBuf.putBytes(0, val);
      }
      tx.commit();
    }

    try (Transaction tx = env.createReadTransaction()) {
      byte[] result = db.get(tx, key);
      assertArrayEquals(result, val);
    }
  }

  @Test
  public void testReserveCursorRollback() {
    byte[] key = new byte[]{1, 1, 1};
    byte[] val = new byte[]{3, 3, 3};

    Transaction tx = env.createWriteTransaction();
    Cursor cursor = db.openCursor(tx);
    DirectBuffer keyBuf = new DirectBuffer(ByteBuffer.allocateDirect(key.length));
    keyBuf.putBytes(0, key);
    DirectBuffer valBuf = cursor.reserve(keyBuf, val.length);
    valBuf.putBytes(0, val);
    tx.abort();

    byte[] result = db.get(key);
    assertNull(result);
  }
}
