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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ValueOrVar;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;

import javax.annotation.Nonnull;

/**
 * Converts to an array
 */
public class ToArrayExpr extends AbstractUnaryExpr {
  public ToArrayExpr(Location location, Expr expr)
  {
    super(location, expr);
  }

  public ToArrayExpr(Expr expr)
  {
    super(expr);
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
  @Nonnull protected V<? extends ValueOrVar> _eval(Env env, FeatureExpr ctx)
  {
    return _expr.eval(env, ctx).map((a) -> a.toArray());
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
  public @Nonnull V<? extends Value> evalCopy(Env env, FeatureExpr ctx)
  {
    V<? extends Value> value = _expr.eval(env, ctx).map((a) -> a.toValue());

    return value.map((v)->
      v instanceof ArrayValue?
      v.copy():
      v.toArray());
  }

  public String toString()
  {
    return "((array) " + _expr + ")";
  }
}

