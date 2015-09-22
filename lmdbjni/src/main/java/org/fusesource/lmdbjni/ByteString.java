package org.fusesource.lmdbjni;

import java.io.UnsupportedEncodingException;


public class ByteString {
  private String string;
  private byte[] bytes;
  private static final String UTF8_CHARSET = "UTF-8";

  public ByteString(String string) {
    this.string = string;
  }

  public ByteString(byte[] bytes) {
    this.bytes = bytes;
  }

  public String getString() {
    if (string == null) {
      try {
        string = new String(bytes, UTF8_CHARSET);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException ("Error getting string", e);
      }
    }
    return string;
  }

  public byte[] getBytes() {
    if (bytes == null) {
      try {
        bytes = string.getBytes(UTF8_CHARSET);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException ("Error getting byte string", e);
      }
    }
    return bytes;
  }

  public int length() {
    return getString().length();
  }

  public int size() {
    return getBytes().length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ByteString that = (ByteString) o;

    if (getString() != null ? !getString().equals(that.getString()) : that.getString() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getString() != null ? getString().hashCode() : 0;
  }
}
