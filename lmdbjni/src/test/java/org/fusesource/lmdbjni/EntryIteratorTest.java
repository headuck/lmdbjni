package org.fusesource.lmdbjni;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class EntryIteratorTest {
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
    db = env.openDatabase();
    keys = new LinkedList<>();
    for (int i = 0; i < 10; i++) {
      byte[] bytes = new byte[]{(byte) i};
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
  public void testEmpty() throws IOException {
    String path = tmp.newFolder().getCanonicalPath();
    env = new Env(path);
    db = env.openDatabase();
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterate(tx)) {
      try {
        it.next();
        fail("Should throw NoSuchElementException");
      } catch (NoSuchElementException e) {
      }
    }
  }

  @Test
  public void testIterateForward() {
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterate(tx)) {
      while (it.hasNext()) {
        Entry next = it.next();
        assertArrayEquals(keys.pollFirst(), next.getKey());
      }
      assertFalse(it.hasNext());
      assertTrue(keys.isEmpty());
    }
  }

  @Test
  public void testNextForward() {
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterate(tx)) {
      for (int i = 0; i < 10; i++) {
        assertArrayEquals(keys.pollFirst(), it.next().getKey());
      }
      try {
        assertNull(it.next());
        fail("should throw NoSuchElementException");
      } catch (NoSuchElementException e) {
      }
    }
    assertTrue(keys.isEmpty());
  }

  @Test
  public void testIterateBackward() {
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterateBackward(tx)) {
      while (it.hasNext()) {
        Entry next = it.next();
        assertArrayEquals(keys.pollLast(), next.getKey());
      }
      assertFalse(it.hasNext());
      assertTrue(keys.isEmpty());
    }
  }

  @Test
  public void testNextBackward() {
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterateBackward(tx)) {
      for (int i = 0; i < 10; i++) {
        assertArrayEquals(keys.pollLast(), it.next().getKey());
      }
      try {
        assertNull(it.next());
        fail("should throw NoSuchElementException");
      } catch (NoSuchElementException e) {
      }
    }
    assertTrue(keys.isEmpty());
  }


  @Test
  public void testSeekForward() {
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.seek(tx, new byte[]{5})) {
      while (it.hasNext()) {
        Entry next = it.next();
        assertArrayEquals(keys.pollFirst(), next.getKey());
      }
      assertFalse(it.hasNext());
      assertTrue(keys.isEmpty());
    }
  }

  @Test
  public void testSeekForwardNext() {
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    keys.pollFirst();
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.seek(tx, new byte[]{5})) {
      for (int i = 0; i < 5; i++) {
        assertArrayEquals(keys.pollFirst(), it.next().getKey());
      }
      try {
        assertNull(it.next());
        fail("should throw NoSuchElementException");
      } catch (NoSuchElementException e) {
      }
    }
    assertTrue(keys.isEmpty());
  }

  @Test
  public void testSeekBackward() {
    keys.pollLast();
    keys.pollLast();
    keys.pollLast();
    keys.pollLast();
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.seekBackward(tx, new byte[]{5})) {
      while (it.hasNext()) {
        Entry next = it.next();
        assertArrayEquals(keys.pollLast(), next.getKey());
      }
      assertFalse(it.hasNext());
      assertTrue(keys.isEmpty());
    }
  }

  @Test
  public void testSeekBackwardNext() {
    keys.pollLast();
    keys.pollLast();
    keys.pollLast();
    keys.pollLast();
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.seekBackward(tx, new byte[]{5})) {
      for (int i = 0; i < 6; i++) {
        assertArrayEquals(keys.pollLast(), it.next().getKey());
      }
      try {
        assertNull(it.next());
        fail("should throw NoSuchElementException");
      } catch (NoSuchElementException e) {
      }
    }
    assertTrue(keys.isEmpty());
  }

  @Test
  public void testIterable() {
    try (Transaction tx = env.createReadTransaction(); EntryIterator it = db.iterate(tx)) {
      Iterator<Entry> iterator = it.iterable().iterator();
      while (iterator.hasNext()) {
        assertArrayEquals(keys.pollFirst(), iterator.next().getKey());
      }
      assertFalse(iterator.hasNext());
    }
    assertTrue(keys.isEmpty());
  }
}
