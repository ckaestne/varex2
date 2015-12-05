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
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.Function;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import javax.annotation.Nonnull;

/**
 * Represents a function definition
 */
public class FunctionDefStatement extends Statement {
  private final static L10N L = new L10N(FunctionDefStatement.class);

  protected Function _fun;

  public FunctionDefStatement(Location location, Function fun)
  {
    super(location);

    _fun = fun;
  }

  public @Nonnull V<? extends Value> execute(Env env, FeatureExpr ctx)
  {
    try {
      String name = _fun.getName();

      env.findFunction(env.createString(name)).vforeach(ctx, (c,fun) -> {
        if (fun == null)
          env.addFunction(c, name, _fun);
        else
          env.error(c, L.l("function {0}() is already defined under condition {1}.", name, c), getLocation());
      });
    }
    catch (RuntimeException e) {
      rethrow(e, RuntimeException.class);
    }

    return V.one(null);
  }
}

