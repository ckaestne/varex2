/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a Quercus output stream
 */
public interface BinaryOutput extends BinaryStream {
  /**
   * Returns an OutputStream.
   */
  OutputStream getOutputStream();
  
  /**
   * Writes a buffer.
   */
  void write(byte[] buffer, int offset, int length)
    throws IOException;

  /**
   * Writes a buffer.
   */
  int write(InputStream is, int length)
    throws IOException;
  
  /**
   * prints a unicode character
   */
  void print(char ch)
    throws IOException;
  
  /**
   * prints
   */
  void print(String s)
    throws IOException;

  /**
   * Flushes the output
   */
  void flush()
    throws IOException;

  /**
   * Closes the stream for writing
   */
  void closeWrite();

  /**
   * Closes the stream.
   */
  @Override
  void close();


}

