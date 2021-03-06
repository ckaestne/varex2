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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ValueOrVar;
import com.caucho.quercus.env.Var;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;

import javax.annotation.Nonnull;

/**
 * Represents the character at expression
 */
public class BinaryCharAtExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(BinaryCharAtExpr.class);

  protected final Expr _objExpr;
  protected final Expr _indexExpr;

  public BinaryCharAtExpr(Location location, Expr objExpr, Expr indexExpr)
  {
    super(location);
    _objExpr = objExpr;
    _indexExpr = indexExpr;
  }
  
  public BinaryCharAtExpr(Expr objExpr, Expr indexExpr)
  {
    _objExpr = objExpr;
    _indexExpr = indexExpr;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  @Nonnull
  protected V<? extends ValueOrVar> _eval(Env env, FeatureExpr ctx)
  {
    V<? extends Value> obj = _objExpr.eval(env, ctx);

    return obj.smap(ctx, (c, a) -> a.charValueAt(_indexExpr.evalLong(env, c).getOne()));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public V<? extends Var> evalVar(Env env, FeatureExpr ctx)
  {
    return eval(env, ctx).map((a) -> a.toVar());
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public V<? extends ValueOrVar> evalArg(Env env, FeatureExpr ctx, boolean isTop)
  {
    return eval(env, ctx);
  }
  
  /**
   * Evaluates the expression as an assignment.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @param value
   * @return the expression value.
   */
  @Override
  public V<? extends ValueOrVar> evalAssignRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar> value)
  {
    Value obj = _objExpr.eval(env, ctx).getOne();

    Value result = obj.setCharValueAt(_indexExpr.evalLong(env, ctx).getOne(),
                                      value.getOne().toValue());

    _objExpr.evalAssignValue(env, ctx, VHelper.toV(result));
    
    return value;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @param ctx
   * @return the expression value.
   */
  @Override
  public void evalUnset(Env env, FeatureExpr ctx)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return _objExpr + "{" + _indexExpr + "}";
  }
}

