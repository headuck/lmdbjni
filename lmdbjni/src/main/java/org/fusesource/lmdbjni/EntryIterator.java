package org.fusesource.lmdbjni;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for entries that follow the same semantics as Cursors
 * with regards to read and write transactions and how they are closed.
 * <p/>
 * <pre>
 * {@code
 * try (EntryIterator it = db.iterate()) {
 *   for (Entry next : it.iterable()) {
 *   }
 * }
 * }
 * </pre>
 */
public class EntryIterator implements Iterator<Entry>, Closeable {
  private final Cursor cursor;
  private final IteratorType type;
  private final byte[] key;

  EntryIterator(Cursor cursor, byte[] key, IteratorType type) {
    this.cursor = cursor;
    this.type = type;
    this.key = key;
  }

  private Entry entry;
  private boolean first = true;

  @Override
  public boolean hasNext() {
    if (first) {
      if (key != null) {
        this.entry = cursor.seek(SeekOp.RANGE, key);
      } else {
        if (type == IteratorType.FORWARD) {
          this.entry = cursor.get(GetOp.FIRST);
        } else {
          this.entry = cursor.get(GetOp.LAST);
        }
      }
      first = false;
      if (entry == null) {
        return false;
      }
    } else {
      if (type == IteratorType.FORWARD) {
        this.entry = cursor.get(GetOp.NEXT);
      } else {
        this.entry = cursor.get(GetOp.PREV);
      }
      if (entry == null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Entry next() throws NoSuchElementException {
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public Iterable<Entry> iterable() {
    return new Iterable<Entry>() {
      @Override
      public Iterator<Entry> iterator() {
        return EntryIterator.this;
      }
    };
  }

  @Override
  public void close() {
    cursor.close();
  }

  enum IteratorType {
    FORWARD, BACKWARD
  }
}
