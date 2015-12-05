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
 * @author Nam Nguyen
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import javax.annotation.Nonnull;

/**
 * Represents a static statement in a PHP program.
 */
public class ClassStaticStatement
  extends Statement
{
  protected final VarExpr _var;
  protected final Expr _initValue;
  protected StringValue _staticName;

  /**
   * Creates the echo statement.
   */
  public ClassStaticStatement(Location location,
                              StringValue staticName,
                              VarExpr var,
                              Expr initValue)
  {
    super(location);

    _staticName = staticName;
    _var = var;
    _initValue = initValue;
  }

  public @Nonnull V<? extends Value> execute(Env env, FeatureExpr ctx)
  {
    try {
      StringValue staticName = _staticName;

      Value qThis = env.getThis();

      QuercusClass qClass = qThis.getQuercusClass();
      String className = qClass.getName();

      // Var var = qClass.getStaticFieldVar(env, env.createString(staticName));
      // Var var = qClass.getStaticFieldVar(env, staticName);
      Var var = env.getStaticVar(env.createString(className
                                                  + "::" + staticName));

      env.setVar(ctx, _var.getName(), V.one(var));

      if (! var.makeValue().isset() && _initValue != null)
        var.set(VHelper.noCtx(), _initValue.eval(env, VHelper.noCtx()));

    }
    catch (RuntimeException e) {
      rethrow(e, RuntimeException.class);
    }

    return V.one(null);
  }
}

