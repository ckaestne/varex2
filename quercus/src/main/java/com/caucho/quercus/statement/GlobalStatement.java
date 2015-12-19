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

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ValueOrVar;
import com.caucho.quercus.expr.VarExpr;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import javax.annotation.Nonnull;

/**
 * Represents a global statement in a PHP program.
 */
public class GlobalStatement extends Statement {
  protected VarExpr _var;
  
  /**
   * Creates the echo statement.
   */
  public GlobalStatement(Location location, VarExpr var)
  {
    super(location);
    
    _var = var;
  }
  
  @Override
  public @Nonnull
  V<? extends ValueOrVar> execute(Env env, FeatureExpr ctx)
  {
    try {
      env.setRef(ctx, _var.getName(), env.getGlobalVar(ctx, _var.getName()));
    }
    catch (RuntimeException e) {
      rethrow(e, RuntimeException.class);
    }

    return V.one(null);
  }
}
