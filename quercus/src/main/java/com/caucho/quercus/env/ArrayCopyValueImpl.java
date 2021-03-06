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

package com.caucho.quercus.env;

import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;

/**
 * Represents a PHP array value copied as part of deserialization or APC.
 *
 * Any modification to the array will set the CopyRoot modified
 */
public class ArrayCopyValueImpl extends ArrayValueImpl
{
  private final CopyRoot _root;

  /**
   * Copy for unserialization.
   *
   * XXX: need to update for references
   */
  protected ArrayCopyValueImpl(Env env, ArrayValue copy, CopyRoot root)
  {
    super(env, copy, root);

    _root = root;
  }

  /**
   * Clears the array
   */
  @Override
  public void clear()
  {
    _root.setModified();

    super.clear();
  }

  /**
   * Adds a new value.
   */
  @Override
  public V<? extends ValueOrVar> put(FeatureExpr ctx, Value key, V<? extends ValueOrVar> value)
  {
    if (_root != null)
      _root.setModified();

    return super.put(ctx, key, value);
  }

  /**
   * Adds a new value.
   */
  @Override
  public ArrayValue append(FeatureExpr ctx, Value index, V<? extends ValueOrVar> value)
  {
    if (_root != null)
      _root.setModified();

    return super.append(ctx, index, value);
  }

  /**
   * Add to the beginning
   */
  @Override
  public ArrayValue unshift(Value value)
  {
    _root.setModified();

    return super.unshift(value);
  }

  /**
   * Replace a section of the array.
   */
  @Override
  public ArrayValue splice(int start, int end, ArrayValue replace)
  {
    _root.setModified();

    return super.splice(start, end, replace);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public EnvVar getArg(Value index, boolean isTop)
  {
    // XXX:
    return super.getArg(index, isTop);
  }

  /**
   * Returns the value as an array, using copy on write if necessary.
   */
  @Override
  public V<? extends Value> getDirty(Value index)
  {
    _root.setModified();

    return super.getDirty(index);
  }

  /**
   * Add
   */
  @Override
  public V<? extends ValueOrVar> put(FeatureExpr ctx, V<? extends ValueOrVar> value)
  {
    _root.setModified();

    return super.put(ctx, value);
  }

  /**
   * Sets the array ref.
   * @param ctx
   */
  @Override
  public V<? extends Var> putVar(FeatureExpr ctx)
  {
    _root.setModified();

    return super.putVar(ctx);
  }

  /**
   * Removes a value.
   */
  @Override
  public V<? extends Value> remove(FeatureExpr ctx, Value key)
  {
    _root.setModified();

    return super.remove(ctx, key);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public EnvVar getVar(FeatureExpr ctx, Value index)
  {
    _root.setModified();

    return super.getVar(ctx, index);
  }

  /**
   * Shuffles the array
   */
  @Override
  public Value shuffle()
  {
    _root.setModified();

    return super.shuffle();
  }

  /**
   * Copy the value.
   */
  @Override
  public Value copy()
  {
    return copy(Env.getInstance());
  }

  /**
   * Convert to an argument value.
   */
  @Override
  public V<? extends Value> toLocalRef()
  {
    return V.one(copy());
  }

  /**
   * Copy for return.
   */
  @Override
  public Value copyReturn()
  {
    return copy();
  }

  /**
   * Copy for saving a method's arguments.
   */
  @Override
  public Value copySaveFunArg()
  {
    return copy();
  }
}
