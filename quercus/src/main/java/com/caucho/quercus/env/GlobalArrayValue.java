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

import com.caucho.inject.Module;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents the server
 */
@Module
public class GlobalArrayValue extends ArrayValueImpl {
  private final Env _env;

  public GlobalArrayValue(Env env)
  {
    _env = env;
  }
  
  /**
   * Converts to an object.
   */
  @Override
  public Object toObject()
  {
    return null;
  }
  
  @Override
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Adds a new value.
   */
  @Override
  public ArrayValue append(FeatureExpr ctx, Value key, V<? extends ValueOrVar> value)
  {
    _env.setGlobalValue(ctx, key.toStringValue(), value);

    return this;
  }

  /**
   * Gets a new value.
   */
  @Override
  public EnvVar get(Value key)
  {
    return new EnvVarImpl(V.one(new VarImpl(_env.getGlobalValue(VHelper.noCtx(), key.toStringValue()))));
  }
  
  /**
   * Returns the array ref.
   */
  @Override
  public EnvVar getVar(FeatureExpr ctx, Value key)
  {
    // return _env.getGlobalRef(key.toStringValue());

    return new EnvVarImpl(_env.getGlobalVar(VHelper.noCtx(), key.toStringValue()));
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public EnvVar getArg(Value index, boolean isTop)
  {
    return getVar(VHelper.noCtx(), index);
  }

  /**
   * Returns the value as an array.
   */
  @Override
  public V<? extends ValueOrVar> getArray(FeatureExpr ctx, Value index)
  {
    V<? extends Var> array = getVar(ctx, index).getVar().map((a) -> a.toAutoArray());

    return array;
  }
  
  /**
   * Unsets a value.
   */
  @Override
  public V<? extends Value> remove(FeatureExpr ctx, Value key)
  {
    return _env.unsetGlobalVar(ctx, key.toStringValue()).map((a) -> a != null ? a.toValue() : null);
  }
  
  @Override
  public void clear()
  {
  }
  
  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return this;
  }
  
  /*
   * Returns the size.
   */
  @Override
  public V<? extends Integer> getSize()
  {
    return V.one(_env.getGlobalEnv().size());
  }
  
  /**
   * Gets a new value.
   */
  @Override
  public V<? extends Value> containsKey(Value key)
  {
    EnvVar var = _env.getGlobalEnv().get(key.toStringValue());

    if (var != null)
      return var.getValue();
    else
      return V.one(null);
  }
  
  /**
   * Returns true if the index isset().
   */
  @Override
  public boolean isset(Value key)
  {
    return get(key).getOne().isset();
  }
  
  /**
   * Returns true if the key exists in the array.
   */
  @Override
  public boolean keyExists(Value key)
  {
    EnvVar var = _env.getGlobalEnv().get(key.toStringValue());
    
    return var != null;
  }
  
  /**
   * Prints the value.
   * @param env
   * @param ctx
   */
  @Override
  public void print(Env env, FeatureExpr ctx)
  {
    env.print(ctx, "Array");
  }

  /**
   * Returns the array keys.
   */
  @Override
  public Value getKeys()
  {
    return createAndFillArray().getKeys();
  }
  
  /**
   * Returns an iterator of the entries.
   */
  @Override
  public Set<VEntry> entrySet()
  {
    return createAndFillArray().entrySet();
  }
  
  @Override
  public Iterator<VEntry> getIterator(Env env)
  {
    return createAndFillArray().getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return createAndFillArray().getKeyIterator(env);
  }

  @Override
  public Iterator<EnvVar> getValueIterator(Env env)
  {
    return createAndFillArray().getValueIterator(env);
  }
  
  private ArrayValue createAndFillArray()
  {
    ArrayValue array = new ArrayValueImpl();

    for (Map.Entry<StringValue, EnvVar> entry : _env.getGlobalEnv()
      .entrySet()) {
      Value key = entry.getKey();
      Value val = entry.getValue().getOne();
      
      array.put(key, val);
    }
    
    return array;
  }
}

