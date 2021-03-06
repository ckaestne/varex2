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

package com.caucho.quercus.lib;

import com.caucho.quercus.Location;
import com.caucho.quercus.annotation.VariableArguments;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.CallExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import edu.cmu.cs.varex.annotation.VParamType;
import edu.cmu.cs.varex.annotation.VVariational;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * PHP function routines.
 */
public class FunctionModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(FunctionModule.class);

  private static final Logger log
    = Logger.getLogger(FunctionModule.class.getName());

  /**
   * Calls a user function
   */
  @VVariational
  @VParamType(Value.class)
  public static V<? extends Value> call_user_func(Env env, FeatureExpr ctx,
                                                  @VParamType(Callable.class) V<? extends Callable> function,
                                                  @VParamType(Value.class) @Nonnull V<? extends Value>[] args)
  {
    return VHelper.getValues(function.getOne().call(env, ctx, args)).
            map((a) -> a.copyReturn());
  }

  /**
   * Calls a user function
   */
  @VVariational
  @VParamType(Value.class)
  public static V<? extends Value> call_user_func_array(Env env, FeatureExpr ctx,
                                                        @VParamType(Callable.class) V<? extends Callable> function,
                                                        @VParamType(Value.class) @Nonnull V<? extends Value> arg)
  {
    if (function == null) {
      env.warning("call_user_func_array: first argument is not a valid function");
      return V.one(ctx, NullValue.NULL);
    }

    ArrayValue argArray;

    if (arg.getOne(ctx).isArray()) {
      argArray = arg.getOne(ctx).toArray();
    }
    else {
      argArray = new ArrayValueImpl().append(arg.getOne(ctx));
    }

    V<? extends ValueOrVar> []args;

    if (argArray != null) {
      args = new V[argArray.getSize().getOne(ctx)];

      int i = 0;

      for (VEntry entry : argArray.entrySet())
        if (entry.getCondition().and(ctx).isSatisfiable()) {
          ArrayValue.Entry arrayEntry = (ArrayValue.Entry) entry;

          args[i++] = arrayEntry.getRawValue().getVar();
        }
    }
    else {
      args = new V[0];
    }

    // nam: 2012-04-30 this works for interpreted, but need to also work for compiled
    // chk: 2015-11-26 fix this, otherwise stackframes and consequently wordpress title is missing
    QuercusClass oldCallingClass = env.getCallingClass();
    env.pushCall(new CallExpr(Location.UNKNOWN, new ConstStringValue(function.getOne(ctx).getCallbackName()), Expr.NULL_ARGS), null, args);

    try {
      return function.sflatMap(ctx, (c, f) -> f.call(env, c, args).flatMap((a) -> a._getValues()).map(a -> a.copyReturn()));
    }
    finally {
      env.popCall();

      env.setCallingClass(oldCallingClass);
    }
  }

  /**
   * Creates an anonymous function
   */
  public static Value create_function(Env env,
                                      String args,
                                      String code)
  {
    try {
      AbstractFunction fun = env.createAnonymousFunction(args, code);

      return new CallbackFunction(fun, env.createString(fun.getName()));
    } catch (IOException e) {
      env.warning(e.getMessage());

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the nth function argument.
   */
  @VariableArguments
  public static Value func_get_arg(Env env, int index)
  {
    V<? extends ValueOrVar>[] args = env.getFunctionArgs();

    if (0 <= index && index < args.length)
      return args[index].getOne().toValue();
    else {
      // XXX: warning
      return NullValue.NULL;
    }
  }

  /**
   * Returns the function arguments as an array.
   */
  @VariableArguments
  public static Value func_get_args(Env env)
  {
    V<? extends ValueOrVar>[] args = env.getFunctionArgs();

    ArrayValue result = new ArrayValueImpl();
    if (args != null) {
      for (int i = 0; i < args.length; i++)
        result.put(VHelper.noCtx(), args[i].getOne().toValue());
    }

    return result;
  }

  /**
   * Returns the number of arguments to the function.
   */
  @VariableArguments
  public static Value func_num_args(Env env)
  {
    V<? extends ValueOrVar>[] args = env.getFunctionArgs();

    if (args != null && args.length > 0)
      return LongValue.create(args.length);
    else
      return LongValue.ZERO;
  }

  /**
   * Returns true if the function exists.
   *
   * @param env the PHP environment
   * @param name the function name
   */
  public static boolean function_exists(Env env, StringValue name)
  {
    return env.findFunction(name).getOne() != null;
  }

  /**
   * Returns an array of the defined functions
   */
  public static Value get_defined_functions(Env env)
  {
    return env.getDefinedFunctions();
  }

  /**
   * Registers a shutdown function.
   */
  public static Value register_shutdown_function(Env env,
                                                 Callable fun,
                                                 Value []args)
  {
    env.addShutdown(fun, args);

    return NullValue.NULL;
  }

  // XXX: register_tick_function
  // XXX: unregister_tick_function
}
