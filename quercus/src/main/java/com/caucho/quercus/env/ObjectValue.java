/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.Location;
import com.caucho.vfs.WriteStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.IdentityHashMap;
import java.io.IOException;

/**
 * Represents a Quercus object value.
 */
abstract public class ObjectValue extends Value {
 transient protected QuercusClass _quercusClass;

  protected ObjectValue(QuercusClass quercusClass)
  {
    _quercusClass = quercusClass;
  }

  protected void setQuercusClass(QuercusClass cl)
  {
    _quercusClass = cl;
  }

  public QuercusClass getQuercusClass()
  {
    return _quercusClass;
  }

  /**
   * Returns the value's class name.
   */
  public String getClassName()
  {
    return _quercusClass.getName();
  }

  /**
   * Returns a Set of entries.
   */
  // XXX: remove entrySet() and use getIterator() instead
  abstract public Set<Map.Entry<String,Value>> entrySet();

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _quercusClass.getName();
  }

  /**
   * Returns the parent class
   */
  public String getParentClassName()
  {
    return _quercusClass.getParentName();
  }

  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return true;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "object";
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return _quercusClass.isA(name);
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return 1;
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toLong();
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    if (rValue.isObject())
      return cmpObject((ObjectValue)rValue) == 0;
    else
      return super.eq(rValue);
  }
  
  /**
   * Compare two objects
   */
  public int cmpObject(ObjectValue rValue)
  {
    if (rValue == this)
      return 0;

    // if objects are not equal, then which object is greater is undefined

    int result = getName().compareTo(rValue.getName());
    
    if (result != 0)
      return result;
    
    Set<Map.Entry<String,Value>> aSet = entrySet();
    Set<Map.Entry<String,Value>> bSet = rValue.entrySet();
    
    if (aSet.equals(bSet))
      return 0;
    else if (aSet.size() > bSet.size())
      return 1;
    else if (aSet.size() < bSet.size())
      return -1;
    else {
      TreeSet<Map.Entry<String,Value>> aTree
      = new TreeSet<Map.Entry<String,Value>>(aSet);

      TreeSet<Map.Entry<String,Value>> bTree
      = new TreeSet<Map.Entry<String,Value>>(bSet);

      Iterator<Map.Entry<String,Value>> iterA = aTree.iterator();
      Iterator<Map.Entry<String,Value>> iterB = bTree.iterator();

      while (iterA.hasNext()) {
        Map.Entry<String,Value> a = iterA.next();
        Map.Entry<String,Value> b = iterB.next();

        result = a.getKey().compareTo(b.getKey());

        if (result != 0)
          return result;
        
        result = a.getValue().cmp(b.getValue());

        if (result != 0)
          return result;
      }

      // should never reach this
      return 0;
    }
  }

  // ArrayDelegate

  @Override
  public int getCount(Env env)
  {
    return _quercusClass.getCount(env, this);
  }

  @Override
  public int getCountRecursive(Env env)
  {
    return _quercusClass.getCountRecursive(env, this);
  }

  @Override
  public Value get(Value key)
  {
    // php/066q vs. php/0906
    //return getField(null, key.toString());

    return _quercusClass.get(Env.getInstance(), this, key);
  }

  @Override
  public Value get(Env env, Location location, Value key)
  {
    return _quercusClass.get(env, this, key);
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return _quercusClass.getIterator(env, this);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return _quercusClass.getKeyIterator(env, this);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return _quercusClass.getValueIterator(env, this);
  }

  @Override
  public Value put(Value key, Value value)
  {
    // php/0d94
    return _quercusClass.put(Env.getInstance(), this, key, value);
  }

  @Override
  public Value put(Env env, Location location, Value key, Value value)
  {
    return _quercusClass.put(env, this, key, value);
  }

  @Override
  public Value put(Value value)
  {
    return _quercusClass.put(Env.getInstance(), this, value);
  }

  @Override
  public Value put(Env env, Location location, Value value)
  {
    return _quercusClass.put(env, this, value);
  }

  @Override
  public Value remove(Value key)
  {
    return _quercusClass.remove(Env.getInstance(), this, key);
  }

  // FieldDelegate

  @Override
  public Value getField(Env env, String name, boolean create)
  {
    return _quercusClass.getField(env, this, name, create);
  }

  @Override
  public Value putField(Env env,
                        String name,
                        Value value)
  {
    return _quercusClass.putField(env, this, name, value);
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, String value)
  {
    return putField(null, key, new StringBuilderValue(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, long value)
  {
    return putField(null, key, LongValue.create(value));
  }

  /**
   * Initializes a new field.
   */
  public Value initField(Env env, String key, Value value)
  {
    return putField(env, key, value);
  }

  /**
   * Adds a new value.
   */
  public Value putField(String key, double value)
  {
    return putField(null, key, DoubleValue.create(value));
  }

  // PrintDelegate

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _quercusClass.printRImpl(env, this,  out, depth, valueSet);
  }

  @Override
  protected void varDumpImpl(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _quercusClass.varDumpImpl(env, this,  out, depth, valueSet);
  }

  @Override
  public void varExport(StringBuilder sb)
  {
    _quercusClass.varExport(Env.getInstance(), this,  sb);
  }
}

