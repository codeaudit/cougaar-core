/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.io.OutputStream;
import java.io.IOException;

/**
 * An efficient implementation of PipedOutputStream
 **/
public class PipedOutputStream extends java.io.OutputStream {
  /**
   * The PipedInputStream this is connected to.
   **/
  private PipedInputStream pis;
  /**
   * Set if this stream has been closed
   **/
  private boolean closed = false;

  /**
   * Create an unconnected PipedOutputStream. To be used this must be
   * passed to a new PipedInputStream or a new PipedInputStream must
   * be connected to this.
   **/
  public PipedOutputStream() {
  }

  /**
   * Create an PipedOutputStream connected to the given
   * PipedInputStream. The pair of streams is immediately ready to be
   * used if this constructor is used.
   * @param pis the PipedInputStream to connect to this.
   **/
  public PipedOutputStream(PipedInputStream pis) {
    this.pis = pis;
  }

  /**
   * Connect an PipedInputStream to this. This must be unconnected.
   * @param pis the PipedInputStream to connect.
   **/
  public synchronized void connect(PipedInputStream pis) {
    if (this.pis != null && this.pis != pis) {
      throw new IllegalArgumentException("Already connected");
    }
    this.pis = pis;
  }

  /**
   * Write a byte to the pipe. The byte is put into the connected
   * PipedInputStream.
   * @param b the byte to put.
   * @exception IOException if this stream is closed or not connected
   * or connected to a closed PipedInputStream.
   **/
  public synchronized void write(int b) throws IOException {
    if (closed) throw new IOException("Closed");
    if (pis == null) throw new IOException("Not Connected");
    pis.put(b);
  }

  /**
   * Write an array of bytes to the pipe. All the bytes in the array
   * are put into the connected PipedInputStream.
   * @param b the byte to put.
   * @exception IOException if this stream is closed or not connected
   * or connected to a closed PipedInputStream.
   **/
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    if (closed) throw new IOException("Closed");
    if (pis == null) throw new IOException("Not Connected");
    pis.put(b, off, len);
  }

  /**
   * Write part of an array of bytes to the pipe. Some of the bytes in
   * the array are put into the connected PipedInputStream.
   * @param b the byte to put.
   * @param off the index of the first byte to put
   * @param len the number of bytes to put
   * @exception IOException if this stream is closed or not connected
   * or connected to a closed PipedInputStream.
   **/
  public synchronized void write(byte[] b) throws IOException {
    if (closed) throw new IOException("Closed");
    if (pis == null) throw new IOException("Not Connected");
    pis.put(b, 0, b.length);
  }

  /**
   * Insure that all bytes that have been written are accessible to
   * the reader of the connected PipedInputStream.
   * @exception IOException if this stream is closed or not connected.
   **/
  public synchronized void flush() throws IOException {
    if (closed) throw new IOException("Closed");
    if (pis == null) throw new IOException("Not Connected");
    pis.flush();
  }

  /**
   * Close this stream. The connection PipedInputStream will signal
   * EOF when all the bytes have been read.
   * @exception IOException if this stream is already closed
   **/
  public synchronized void close() throws IOException {
    if (closed) throw new IOException("Closed");
    if (pis != null) {
      pis.setEOF();
    }
    closed = true;
  }
}
