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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.VHelper;

public class ArrayValueMarshal extends Marshal
{
  public static final Marshal MARSHAL = new ArrayValueMarshal();
  
  @Override
  public boolean isReference()
  {
    return true;
  }
      
  @Override
  public boolean isReadOnly()
  {
    return false;
  }
      
  /**
   * Return true if is a Value.
   */
  @Override
  public boolean isValue()
  {
    return true;
  }
  
  @Override
  public Object marshal(Env env, FeatureExpr ctx, Expr expr, Class expectedClass)
  {
    return expr.eval(env, VHelper.noCtx()).getOne().toArrayValue(env);
  }

  @Override
  public Object marshalValue(Env env, FeatureExpr ctx, Value value, Class expectedClass)
  {
    return value.toArrayValue(env);
  }

  @Override
  public Value unmarshal(Env env, FeatureExpr ctx, Object value)
  {
    if (value instanceof ArrayValue)
      return (ArrayValue) value;
    else if (value instanceof Value)
      return ((Value) value).toArrayValue(env);
    else
      return NullValue.NULL;
  }
  
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (argValue.isArray()) {
      if (argValue instanceof JavaAdapter)
        return Marshal.ONE;
      else
        return Marshal.ZERO;
    }
    else
      return Marshal.FOUR;
  }
  
  @Override
  public Class getExpectedClass()
  {
    return ArrayValue.class;
  }
}
