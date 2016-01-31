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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.ValueOrVar;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP static field reference.
 */
public class ClassFieldVarExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ClassFieldVarExpr.class);

  protected final String _className;
  protected final Expr _varName;

  public ClassFieldVarExpr(String className, Expr varName)
  {
    _className = className;

    _varName = varName;
  }

  //
  // function call creation
  //

  /**
   * Creates a function call expression
   */
  @Override
  public Expr createCall(QuercusParser parser,
                         Location location,
                         ArrayList<Expr> args)
    throws IOException
  {
    ExprFactory factory = parser.getExprFactory();

    Expr var = factory.createVarVar(_varName);

    return factory.createClassMethodCall(location, _className, var, args);
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
    V<? extends StringValue> varName = _varName.evalStringValue(env, ctx);

    return varName.flatMap((a)->env.getClass(_className).getStaticFieldValue(env, a));
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
    V<? extends StringValue> varName = _varName.evalStringValue(env, ctx);

    return varName.flatMap((a)-> env.getClass(_className).getStaticFieldVar(env, a));
  }

  /**
   * Evaluates the expression.
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
    V<? extends StringValue> varName = _varName.evalStringValue(env, ctx);

    env.getClass(_className).setStaticFieldRef(env, ctx, varName.getOne(), value);

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
    env.error(L.l("{0}::${1}: Cannot unset static variables.",
            _className, _varName.evalStringValue(env, ctx)),
              getLocation());
  }

  public String toString()
  {
    return _className + "::$" + _varName;
  }
}

