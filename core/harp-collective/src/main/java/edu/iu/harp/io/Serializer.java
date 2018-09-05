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

package edu.iu.harp.io;

import edu.iu.harp.resource.ByteArray;
import org.apache.log4j.Logger;

import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * A class for serialization implemented
 * DataOutput interface
 ******************************************************/
public class Serializer implements DataOutput {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(Serializer.class);

  /**
   * the output stream
   */
  private byte[] bytes;
  private int pos;
  private int len;

  public Serializer(ByteArray byteArr) {
    this(byteArr.get(), byteArr.start(), byteArr.start() + byteArr.size());
  }

  /**
   * Serialize on bytes which starts from bytes[0]
   * to bytes[len - 1], with pos between 0 ~ (len
   * - 1)
   *
   * @param bytes the byte[]
   * @param len   the length of the byte[]
   * @param pos   the current position in the byte[]
   */
  public Serializer(byte[] bytes, int pos, int len) {
    this.bytes = bytes;
    this.pos = pos;
    this.len = len;
  }

  /**
   * Get the current position
   *
   * @return
   */
  public int getPos() {
    return this.pos;
  }

  /**
   * Get the length
   *
   * @return the length
   */
  public int getLength() {
    return this.len;
  }

  /**
   * Write the value to the output stream
   */
  @Override
  public void write(int b) throws IOException {
    b = b & 0xFF;
    writeByte(b);
  }

  /**
   * Write the byte[] to the output stream
   */
  @Override
  public void write(byte[] b) throws IOException {
    if ((pos + b.length) > len) {
      throw new IOException("Cannot write.");
    }
    System.arraycopy(b, 0, bytes, pos, b.length);
    pos += b.length;
  }

  /**
   * Write the byte[] of the length beginning from
   * the off to the output stream
   */
  @Override
  public void write(byte[] b, int off, int length) throws IOException {
    if (((pos + length) > len)
        || ((off + length) > b.length)) {
      throw new IOException("Cannot write.");
    }
    System.arraycopy(b, off, bytes, pos, length);
    pos += length;
  }

  /**
   * Write the boolean value to the output stream
   */
  @Override
  public void writeBoolean(boolean v) throws IOException {
    if (pos >= len) {
      throw new IOException("Cannot write.");
    }
    if (v) {
      bytes[pos++] = 1;
    } else {
      bytes[pos++] = 0;
    }
  }

  /**
   * Write the byte value to the output stream
   */
  @Override
  public void writeByte(int v) throws IOException {
    if (pos >= len) {
      throw new IOException("Cannot write.");
    }
    bytes[pos++] = (byte) (v & 0xFF);
  }

  /**
   * Write the short value to the output stream
   */
  @Override
  public void writeShort(int v) throws IOException {
    if ((pos + 2) > len) {
      throw new IOException("Cannot write.");
    }
    bytes[pos++] = (byte) ((v >>> 8) & 0xFF);
    bytes[pos++] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write the char value to the output stream
   */
  @Override
  public void writeChar(int v) throws IOException {
    if ((pos + 2) > len) {
      throw new IOException("Cannot write.");
    }
    bytes[pos++] = (byte) ((v >>> 8) & 0xFF);
    bytes[pos++] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write the int value to the output stream
   */
  @Override
  public void writeInt(int v) throws IOException {
    if ((pos + 4) > len) {
      throw new IOException("Cannot write.");
    }
    bytes[pos++] = (byte) ((v >>> 24) & 0xFF);
    bytes[pos++] = (byte) ((v >>> 16) & 0xFF);
    bytes[pos++] = (byte) ((v >>> 8) & 0xFF);
    bytes[pos++] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write the long value to the output stream
   */
  @Override
  public void writeLong(long v) throws IOException {
    if ((pos + 8) > len) {
      throw new IOException("Cannot write long.");
    }
    bytes[pos++] = (byte) ((v >>> 56) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 48) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 40) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 32) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 24) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 16) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 8) & 0xffL);
    bytes[pos++] = (byte) ((v >>> 0) & 0xffL);
  }

  /**
   * Write the float value to the output stream
   */
  @Override
  public void writeFloat(float v) throws IOException {
    writeInt(Float.floatToIntBits(v));
  }

  /**
   * Write the double value to the output stream
   */
  @Override
  public void writeDouble(double v) throws IOException {
    writeLong(Double.doubleToLongBits(v));
  }

  /**
   * Write the String value to the output stream
   */
  @Override
  public void writeBytes(String s) throws IOException {
    writeChars(s);
  }

  /**
   * Write the String value to the output stream
   */
  @Override
  public void writeChars(String s)
      throws IOException {
    int length = s.length();
    if ((pos + 4 + length * 2) > len) {
      throw new IOException("Cannot write.");
    }
    writeInt(length);
    for (int i = 0; i < length; i++) {
      writeChar(s.charAt(i));
    }
  }

  /**
   * Write the String value to the output stream
   */
  @Override
  public void writeUTF(String s) throws IOException {
    writeChars(s);
  }
}
