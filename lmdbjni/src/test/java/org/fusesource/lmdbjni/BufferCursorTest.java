package org.fusesource.lmdbjni;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BufferCursorTest {
  static {
    Setup.setLmdbLibraryPath();
  }

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Env env;
  Database db;
  LinkedList<byte[]> keys;

  @Before
  public void before() throws IOException {
    String path = tmp.newFolder().getCanonicalPath();
    env = new Env(path);
    db = env.openDatabase("test", Constants.DUPSORT | Constants.CREATE);
    keys = new LinkedList<>();
    for (int i = 1; i < 10; i++) {
      byte[] bytes = new byte[]{(byte) i};
      keys.add(bytes);
      db.put(bytes, bytes);
    }

    for (int i = 0; i < 10; i++) {
      byte[] bytes = Bytes.fromLong(i);
      keys.add(bytes);
      db.put(bytes, bytes);
    }
  }

  @After
  public void after() {
    db.close();
    env.close();
  }

  @Test
  public void testBufferCursor() {
    Transaction tx = env.createReadTransaction();
    try (BufferCursor cursor = db.bufferCursor(tx)) {
      assertTrue(cursor.first());
      assertThat(cursor.keyLong(0), is(0L));
      // go to far
      assertFalse(cursor.prev());
      assertFalse(cursor.prev());
      assertThat(cursor.keyLength(), is(0));
      assertThat(cursor.valLength(), is(0));

      // Position cursor at key >= 0x00
      assertTrue(cursor.seekRange(new byte[]{0}));
      assertThat(cursor.keyLength(), is(8));
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valLength(), is(8));

      // Position cursor at key = 0x00
      cursor.keyWriteByte(0);
      assertFalse(cursor.seekKey());
      assertThat(cursor.keyLength(), is(0));
      assertThat(cursor.valLength(), is(0));

      // Position cursor at key >= 0x01
      assertTrue(cursor.seekRange(new byte[]{1}));
      assertThat(cursor.keyLength(), is(1));
      assertThat(cursor.keyByte(0), is((byte) 1));

      cursor.keyWriteByte(1);
      assertTrue(cursor.seekKey());
      assertThat(cursor.keyByte(0), is((byte) 1));

      assertTrue(cursor.seekRange(Bytes.fromLong(1)));
      assertThat(cursor.keyLong(0), is(1L));

      cursor.keyWriteByte(1);
      assertTrue(cursor.seekKey());
      assertThat(cursor.keyByte(0), is((byte) 1));

      cursor.keyWriteLong(1);
      assertTrue(cursor.seekKey());
      assertThat(cursor.keyLong(0), is(1L));

      cursor.keyWriteByte(0);
      assertFalse(cursor.seekKey());

      cursor.keyWriteLong(keys.size() + 1);
      assertFalse(cursor.seekKey());

      assertTrue(cursor.seekRange(new byte[]{1}));
      assertThat(cursor.keyByte(0), is((byte) 1));
      assertTrue(cursor.next());
      assertThat(cursor.keyByte(0), is((byte) 2));
      assertTrue(cursor.prev());
      assertThat(cursor.keyByte(0), is((byte) 1));

      assertTrue(cursor.last());
      assertThat(cursor.keyByte(0), is((byte) 9));
      assertTrue(cursor.prev());
      assertThat(cursor.keyByte(0), is((byte) 8));
      // go too far
      assertTrue(cursor.next());
      assertThat(cursor.keyByte(0), is((byte) 9));
      assertFalse(cursor.next());

      assertTrue(cursor.seekRange(new byte[]{5}));
      assertThat(cursor.keyByte(0), is((byte) 5));
      assertThat(cursor.valByte(0), is((byte) 5));


      assertTrue(cursor.first());
      long expected = 1;
      while (cursor.next()) {
        if (expected > 9) {
          break;
        }
        assertThat(cursor.keyLong(0), is(expected++));
      }

      assertTrue(cursor.last());
      expected = 8;
      while (cursor.prev()) {
        if (expected < 1) {
          break;
        }
        assertThat(cursor.keyByte(0), is((byte) expected--));
      }
    }
    tx.commit();
  }

  @Test
  public void testUnpositionedCursor() {
    // Next on new cursor points to the first entry
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        // Key is empty when cursor is not positionned
        assertThat(cursor.keyLength(), is(0));
        // Value is empty when cursor is not positionned
        assertThat(cursor.valLength(), is(0));
        // We get an IndexOutOfBoundsException when we try to access unpositioned key data
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyByte(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyShort(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyInt(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyLong(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyFloat(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyDouble(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyUtf8(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyBytes();
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyBytes(0, 1);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.keyDirectBuffer();
          }
        });
        // We get an IndexOutOfBoundsException when we try to access unpositioned value data
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valByte(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valShort(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valInt(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valLong(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valFloat(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valDouble(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valUtf8(0);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valBytes();
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valBytes(0, 1);
          }
        });
        assertIndexOutOfBounds(new Runnable() {
          @Override
          public void run() {
            cursor.valDirectBuffer();
          }
        });
      }
    }

    // Next on new cursor points to the first entry
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        assertTrue(cursor.next());
        assertThat(cursor.keyLength(), is(8));
        assertThat(cursor.keyLong(0), is(0L));
        assertThat(cursor.valLength(), is(8));
        assertThat(cursor.valLong(0), is(0L));
      }
    }
    // Prev on new cursor points to the first entry
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        assertTrue(cursor.prev());
        assertThat(cursor.keyLength(), is(1));
        assertThat(cursor.keyByte(0), is((byte) 9));
        assertThat(cursor.valLength(), is(1));
        assertThat(cursor.valByte(0), is((byte) 9));
      }
    }
  }

  @Test
  public void testDelete() {
    Transaction tx = env.createWriteTransaction();
    try (BufferCursor cursor = db.bufferCursor(tx)) {
      assertTrue(cursor.first());
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valLong(0), is(0L));
      cursor.next();
      assertThat(cursor.keyLong(0), is(1L));
      assertThat(cursor.valLong(0), is(1L));
      cursor.delete();
      // the buffer still holds the value just deleted
      assertThat(cursor.keyLong(0), is(1L));
      assertThat(cursor.valLong(0), is(1L));
      cursor.next();
      assertThat(cursor.keyLong(0), is(2L));
      assertThat(cursor.valLong(0), is(2L));
      cursor.prev();
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valLong(0), is(0L));
    }
    tx.commit();
  }

  @Test
  public void testOverwrite() {
    Transaction tx = env.createWriteTransaction();
    try (BufferCursor cursor = db.bufferCursor(tx)) {
      assertTrue(cursor.seekRange(Bytes.fromLong(0)));
      cursor.delete();
      assertTrue(cursor.keyWriteLong(0).valWriteByte(100).overwrite());
      assertTrue(cursor.first());
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valByte(0), is((byte) 100));
      assertTrue(cursor.valWriteByte(200).overwrite());
      assertTrue(cursor.first());
      assertTrue(cursor.nextDup());
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valByte(0), is((byte) 200));
    }
    tx.commit();
    tx = env.createWriteTransaction();
    try (BufferCursor cursor = db.bufferCursor(tx)) {
      assertTrue(cursor.first());
      assertTrue(cursor.nextDup());
      assertThat(cursor.keyLong(0), is(0L));
      assertThat(cursor.valByte(0), is((byte) 200));
    }
    tx.commit();
  }

  @Test
  public void testAppend() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.keyWriteByte(100).valWriteByte(100).append();
        assertTrue(cursor.first());
        assertThat(cursor.keyLong(0), is(0L));
        assertThat(cursor.valLong(0), is(0L));
        assertTrue(cursor.last());
        assertThat(cursor.keyByte(0), is((byte) 100L));
        assertThat(cursor.valByte(0), is((byte) 100L));
      }
      tx.commit();
    }

    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        assertTrue(cursor.first());
        assertThat(cursor.keyLong(0), is(0L));
        assertThat(cursor.valLong(0), is(0L));
        assertTrue(cursor.last());
        assertThat(cursor.keyByte(0), is((byte) 100));
        assertThat(cursor.valByte(0), is((byte) 100L));
      }
      tx.commit();
    }
  }

  @Test
  public void testDirectBuffer() {
    DirectBuffer key = new DirectBuffer(ByteBuffer.allocateDirect(10));
    DirectBuffer value = new DirectBuffer(ByteBuffer.allocateDirect(10));
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, key, value)) {
        cursor.setWriteMode();
        key.putString(0, new ByteString("a"));
        value.putString(0, new ByteString("a"));
        cursor.put();
        cursor.last();
        assertThat(cursor.keyUtf8(0).getString(), is("a"));
        assertThat(cursor.valUtf8(0).getString(), is("a"));
      }
      tx.commit();
    }
    key = new DirectBuffer(ByteBuffer.allocateDirect(10));
    value = new DirectBuffer(ByteBuffer.allocateDirect(10));
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, key, value)) {
        assertTrue(cursor.last());
        assertThat(cursor.keyUtf8(0).getString(), is("a"));
        assertThat(cursor.valUtf8(0).getString(), is("a"));
        assertThat(cursor.seekKey(), is(true));
        assertThat(cursor.keyUtf8(0).getString(), is("a"));
        assertThat(cursor.valUtf8(0).getString(), is("a"));
        cursor.setWriteMode();
        // Position at key = 'Z\0'
        key.putString(0, new ByteString("Z"));
        assertThat(cursor.seekKey(), is(false));
        // Position at key >= 'Z\0'
        assertThat(cursor.seekRange(), is(true));
        assertThat(cursor.keyUtf8(0).getString(), is("a"));
        assertThat(cursor.valUtf8(0).getString(), is("a"));
      }
    }
  }


  @Test
  public void testPut() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        assertTrue(cursor.seekRange(new byte[]{1}));
        assertFalse(cursor
          .keyWriteByte(1)
          .valWriteByte(100)
          .put());
        assertTrue(cursor.seekRange(new byte[]{1}));
        assertTrue(cursor.first());
        assertThat(cursor.keyLong(0), is(0L));
        assertThat(cursor.valLong(0), is(0L));
        assertTrue(cursor
          .keyWriteByte(111)
          .valWriteByte(121)
          .put());
        assertTrue(cursor.seekRange(new byte[]{111}));
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valByte(0), is((byte) 121));
      }
      tx.commit();
    }
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        assertTrue(cursor.seekRange(new byte[]{111}));
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valByte(0), is((byte) 121));
      }
      tx.commit();
    }
  }

  @Test
  public void testDup() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.keyWriteUtf8("abc").valWriteUtf8("1")
          .overwrite();
        cursor.keyWriteUtf8("abc").valWriteUtf8("2")
          .overwrite();
        cursor.keyWriteUtf8("abc").valWriteUtf8("3")
          .overwrite();
        assertTrue(cursor.last());
        assertThat(cursor.keyUtf8(0).getString(), is("abc"));
        assertThat(cursor.valUtf8(0).getString(), is("3"));
        assertTrue(cursor.prevDup());
        assertThat(cursor.keyUtf8(0).getString(), is("abc"));
        assertThat(cursor.valUtf8(0).getString(), is("2"));
        assertTrue(cursor.prevDup());
        assertThat(cursor.keyUtf8(0).getString(), is("abc"));
        assertThat(cursor.valUtf8(0).getString(), is("1"));
        assertFalse(cursor.prevDup());
        assertTrue(cursor.lastDup());
        assertThat(cursor.keyUtf8(0).getString(), is("abc"));
        assertThat(cursor.valUtf8(0).getString(), is("3"));
        assertTrue(cursor.firstDup());
        assertTrue(cursor.firstDup());
        assertThat(cursor.keyUtf8(0).getString(), is("abc"));
        assertThat(cursor.valUtf8(0).getString(), is("1"));
      }
      tx.commit();
    }
  }

  @Test
  public void testWriteDataTypes() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.first();
        cursor.keyWriteByte(111)
          .keyWriteBoolean(true)
          .keyWriteShort((short) 1)
          .keyWriteInt(2)
          .keyWriteLong(3)
          .keyWriteFloat(1.0f)
          .keyWriteDouble(2.0)
          .keyWriteBytes(new byte[]{1, 2, 3})
          .keyWriteBytes(new byte[]{4, 5, 6}, 1, 1)
          .keyWriteUtf8(new ByteString("abc"))
          .valWriteByte(112)
          .valWriteBoolean(false)
          .valWriteShort((short) 2)
          .valWriteInt(3)
          .valWriteLong(4)
          .valWriteFloat(5.0f)
          .valWriteDouble(6.0)
          .valWriteBytes(new byte[]{1, 2, 3})
          .valWriteBytes(new byte[]{4, 5, 6}, 1, 1)
          .valWriteUtf8("cba");
        assertThat(cursor.keyWriteIndex(), is(36));
        assertThat(cursor.valWriteIndex(), is(36));
        cursor.overwrite();
      }
      tx.commit();
    }

    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte)111));
        assertThat(cursor.keyBoolean(1), is(true));
        assertThat(cursor.keyShort(2), is((short) 1));
        assertThat(cursor.keyInt(4), is(2));
        assertThat(cursor.keyLong(8), is(3L));
        assertThat(cursor.keyFloat(16), is(1.0f));
        assertThat(cursor.keyDouble(20), is(2.0));
        assertArrayEquals(cursor.keyBytes(28, 3), new byte[]{1, 2, 3});
        assertArrayEquals(cursor.keyBytes(31, 1), new byte[]{5});
        assertThat(cursor.keyUtf8(32).getString(), is("abc"));
        assertThat(cursor.keyDirectBuffer().getDouble(20, ByteOrder.BIG_ENDIAN), is(2.0d));
        assertThat(cursor.valByte(0), is((byte) 112));
        assertThat(cursor.valBoolean(1), is(false));
        assertThat(cursor.valShort(2), is((short) 2));
        assertThat(cursor.valInt(4), is(3));
        assertThat(cursor.valLong(8), is(4L));
        assertThat(cursor.valFloat(16), is(5.0f));
        assertThat(cursor.valDouble(20), is(6.0));
        assertArrayEquals(cursor.valBytes(28, 3), new byte[]{1, 2, 3});
        assertArrayEquals(cursor.valBytes(31, 1), new byte[]{5});
        assertThat(cursor.valUtf8(32), is(new ByteString("cba")));
        assertThat(cursor.valDirectBuffer().getDouble(20, ByteOrder.BIG_ENDIAN), is(6.0d));
      }
      tx.commit();
    }
  }

  @Test
  public void testValueBufferExpansionByte() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteByte(112).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valByte(0), is((byte) 112));
      }
    }
  }

  @Test
  public void testValueBufferExpansionShort() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteShort((short) 3).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valShort(0), is((short) 3));
      }
    }
  }

  @Test
  public void testValueBufferExpansionInt() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteInt(3).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valInt(0), is(3));
      }
    }
  }

  @Test
  public void testValueBufferExpansionLong() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteLong(3).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valLong(0), is(3L));
      }
    }
  }

  @Test
  public void testValueBufferExpansionFloat() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteFloat(3.0f).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valFloat(0), is(3.0f));
      }
    }
  }

  @Test
  public void testValueBufferExpansionDouble() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteDouble(3.0d).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valDouble(0), is(3.0d));
      }
    }
  }

  @Test
  public void testValueBufferExpansionBytes() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteBytes(new byte[]{1, 2, 3}).overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertArrayEquals(cursor.valBytes(), new byte[]{1, 2, 3});
      }
    }
  }

  @Test
  public void testValueBufferExpansionUtf8() {
    try (Transaction tx = env.createWriteTransaction(); ) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        cursor.keyWriteByte(111).valWriteUtf8("abc").overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        assertThat(cursor.valUtf8(0).getString(), is("abc"));
      }
    }
  }

  @Test
  public void testValueBufferExpansionDirectBuffer() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx, 1)) {
        cursor.first();
        DirectBuffer directBuffer = new DirectBuffer(ByteBuffer.allocateDirect(10));
        directBuffer.putLong(0, 111L);
        cursor.keyWriteByte(111)
          .valWrite(directBuffer, 8)
          .valWrite(directBuffer, 8)
          .valWrite(directBuffer, 8)
          .overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        assertThat(cursor.keyByte(0), is((byte) 111));
        DirectBuffer directBuffer = cursor.valDirectBuffer();
        assertThat(directBuffer.capacity(), is(8 * 3));
        assertThat(directBuffer.getLong(0), is(111L));
        assertThat(directBuffer.getLong(8), is(111L));
        assertThat(directBuffer.getLong(16), is(111L));
      }
    }
  }

  @Test
  public void testByteString() {
    ByteString string = new ByteString("cba");
    assertThat(string.length(), is(3));
    HashMap<ByteString, ByteString> map = new HashMap<>();
    map.put(string, string);
    assertThat(string, is(new ByteString("cba")));
  }


  @Test
  public void testWriteStrings() {
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.first();
        cursor.keyWriteUtf8("abc")
          .keyWriteUtf8("def")
          .valWriteUtf8("ghi")
          .valWriteUtf8(new ByteString("jkl"))
          .overwrite();
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        ByteString string = cursor.keyUtf8(0);
        assertThat(string.getString(), is("abc"));
        // add NULL byte
        string = cursor.keyUtf8(string.size() + 1);
        assertThat(string.getString(), is("def"));
        string = cursor.valUtf8(0);
        assertThat(string.getString(), is("ghi"));
        // add NULL byte
        string = cursor.valUtf8(string.size() + 1);
        assertThat(string, is(new ByteString("jkl")));
      }
    }
  }

  @Test
  public void testWriteBuffers() {
    ByteString stringKey = new ByteString("key");
    ByteString stringValue = new ByteString("value");
    try (Transaction tx = env.createWriteTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.first();
        DirectBuffer key = new DirectBuffer();
        key.putString(0, stringKey);
        // remember NULL byte
        cursor.keyWrite(key, stringKey.size() + 1);
        cursor.valWriteInt(12);
        DirectBuffer value = new DirectBuffer();
        value.putString(0, stringValue);
        // remember NULL byte
        cursor.valWrite(value, stringValue.size() + 1);
        cursor.put();
        debug(cursor);
      }
      tx.commit();
    }
    try (Transaction tx = env.createReadTransaction()) {
      try (BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.last();
        ByteString string = cursor.keyUtf8(0);
        assertThat(string.getString(), is("key"));
        assertThat(cursor.valInt(0), is(12));
        assertThat(cursor.valUtf8(4).getString(), is("value"));
      }
    }
  }

  @Test
  public void testWriteToReadOnlyBuffer() {
    try (Transaction tx = env.createReadTransaction()) {
      try (final BufferCursor cursor = db.bufferCursor(tx)) {
        cursor.keyWriteByte(0);
        cursor.keyWriteShort((short) 0);
        cursor.keyWriteInt(0);
        cursor.keyWriteLong(0);
        cursor.keyWriteFloat(0);
        cursor.keyWriteDouble(0);
        cursor.keyWriteUtf8("");
        cursor.keyWriteUtf8(new ByteString(""));
        cursor.keyWriteBytes(new byte[]{0});
        cursor.keyWrite(new DirectBuffer(new byte[0]), 0);
        assertEACCES(new Runnable() { public void run() { cursor.valWriteByte(0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteShort((short) 0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteInt(0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteLong(0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteFloat(0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteDouble(0); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteUtf8(""); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteUtf8(new ByteString("")); }});
        assertEACCES(new Runnable() { public void run() { cursor.valWriteBytes(new byte[]{0});}});
        assertEACCES(new Runnable() { public void run() { cursor.valWrite(new DirectBuffer(new byte[0]), 0);}});
      }
    }
  }

  private void debug(BufferCursor cursor) {
    System.out.println("----");
    cursor.first();
    System.out.println(Arrays.toString(cursor.keyBytes()) + " " + Arrays.toString(cursor.valBytes()));
    while (cursor.next()) {
      System.out.println(Arrays.toString(cursor.keyBytes()) + " " + Arrays.toString(cursor.valBytes()));
    }
    System.out.println("----");
  }

  private void assertIndexOutOfBounds(Runnable runnable) {
    try {
      runnable.run();
      fail("should throw IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      assertThat(e.getMessage(), is("Cursor is in an unpositioned state"));
    }
  }

  private void assertEACCES(Runnable runnable) {
    try {
      runnable.run();
      fail("should throw EACCES");
    } catch (LMDBException e) {
      assertThat(e.getErrorCode(), is(LMDBException.EACCES));
    }
  }
}
