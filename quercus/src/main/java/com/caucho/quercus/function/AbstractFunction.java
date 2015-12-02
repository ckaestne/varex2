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

package com.caucho.quercus.function;

import com.caucho.quercus.Location;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.Visibility;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a function
 */
@SuppressWarnings("serial")
abstract public class AbstractFunction extends Callback {
  private static final L10N L = new L10N(AbstractFunction.class);

  public static final Arg []NULL_ARGS = new Arg[0];
  private static final V<? extends ValueOrVar> []NULL_ARG_VALUES = new V[0];

  private final Location _location;

  private boolean _isGlobal = true;
  protected boolean _isStatic = false;
  protected boolean _isFinal = false;
  protected boolean _isConstructor = false;
  protected boolean _isClosure = false;

  protected boolean _isTraitMethod = false;

  protected Visibility _visibility = Visibility.PUBLIC;
  protected String _declaringClassName;

  protected QuercusClass _bindingClass;

  protected int _parseIndex;

  public AbstractFunction()
  {
    // XXX:
    _location = Location.UNKNOWN;
  }

  public AbstractFunction(Location location)
  {
    _location = location;
  }

  public String getName()
  {
    return "unknown";
  }

  //
  // Callback values
  //

  @Override
  public String getCallbackName()
  {
    return getName();
  }

  @Override
  public boolean isInternal(Env env)
  {
    return false;
  }

  @Override
  public boolean isValid(Env env)
  {
    return true;
  }

  /**
   * Returns the name of the file where this is defined in.
   */
  @Override
  public String getDeclFileName(Env env)
  {
    return _location.getFileName();
  }

  /**
   * Returns the start line in the file where this is defined in.
   */
  @Override
  public int getDeclStartLine(Env env)
  {
    return _location.getLineNumber();
  }

  /**
   * Returns the end line in the file where this is defined in.
   */
  @Override
  public int getDeclEndLine(Env env)
  {
    return _location.getLineNumber();
  }

  /**
   * Returns the comment in the file where this is defined in.
   */
  @Override
  public String getDeclComment(Env env)
  {
    return getComment();
  }

  /**
   * Returns true if this returns a reference.
   */
  @Override
  public boolean isReturnsReference(Env env)
  {
    return true;
  }

  @Override
  public Arg []getArgs(Env env)
  {
    return NULL_ARGS;
  }

  public boolean isJavaMethod()
  {
    return false;
  }

  public final String getCompilationName()
  {
    String compName = getName() + "_" + _parseIndex;

    compName = compName.replace("__", "___");
    compName = compName.replace("\\", "__");

    return compName;
  }

  /**
   * Returns the name of class lexically declaring the method
   */
  public String getDeclaringClassName()
  {
    return _declaringClassName;
  }

  public void setDeclaringClassName(String name)
  {
    _declaringClassName = name;
  }

  /**
   * Returns the name of class lexically binding the method
   */
  public String getBindingClassName()
  {
    if (_bindingClass != null)
      return _bindingClass.getName();
    else
      return "<none>";
  }

  public void setBindingClass(QuercusClass qcl)
  {
    _bindingClass = qcl;
  }

  public QuercusClass getBindingClass()
  {
    return _bindingClass;
  }

  /**
   * Returns the implementing class.
   */
  public ClassDef getDeclaringClass()
  {
    return null;
  }

  /**
   * Returns true for a global function.
   */
  public final boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * Returns true for an abstract function.
   */
  public boolean isAbstract()
  {
    return false;
  }

  /**
   * Sets true if function is static.
   */
  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  /**
   * Returns true for a static function.
   */
  public boolean isStatic()
  {
    return _isStatic;
  }

  /**
   * Returns true for a final function.
   */
  public boolean isFinal()
  {
    return _isFinal;
  }

  public final void setFinal(boolean isFinal)
  {
    _isFinal = isFinal;
  }

  /**
   * Sets true if function is a closure.
   */
  public void setClosure(boolean isClosure)
  {
    _isClosure = isClosure;
  }

  /**
   * Returns true for a closure.
   */
  public boolean isClosure()
  {
    return _isClosure;
  }

  /**
   * Returns true for a constructor.
   */
  public boolean isConstructor()
  {
    return _isConstructor;
  }

  /**
   * True for a constructor.
   */
  public final void setConstructor(boolean isConstructor)
  {
    _isConstructor = isConstructor;
  }

  /**
   * Returns true for a trait method.
   */
  public boolean isTraitMethod()
  {
    return _isTraitMethod;
  }

  /**
   * True for a trait method.
   */
  public void setTraitMethod(boolean isTraitMethod)
  {
    _isTraitMethod = isTraitMethod;
  }

  /**
   * Returns true for a protected function.
   */
  public boolean isPublic()
  {
    return _visibility == Visibility.PUBLIC;
  }

  /**
   * Returns true for a protected function.
   */
  public boolean isProtected()
  {
    return _visibility == Visibility.PROTECTED;
  }

  /**
   * Returns true for a private function.
   */
  public boolean isPrivate()
  {
    return _visibility == Visibility.PRIVATE;
  }

  public final void setVisibility(Visibility v)
  {
    _visibility = v;
  }

  public final void setParseIndex(int index)
  {
    _parseIndex = index;
  }

  public final Location getLocation()
  {
    return _location;
  }

  /**
   * Returns true for a global function.
   */
  public final void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }

  /**
   * Returns true for a boolean function.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true for a string function.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true for a long function.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true for a double function.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true if the function uses variable args.
   */
  public boolean isCallUsesVariableArgs()
  {
    return false;
  }

  /**
   * Returns true if the function uses/modifies the local symbol table
   */
  public boolean isCallUsesSymbolTable()
  {
    return false;
  }

  /**
   * Returns the args.
   */
  public Arg []getClosureUseArgs()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the args.
   */
  public void setClosureUseArgs(Arg []useArgs)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * For lazy functions, returns the actual function
   */
  public AbstractFunction toFun()
  {
    return this;
  }

  /**
   * Returns the actual function
   */
  public AbstractFunction getActualFunction(Expr []args)
  {
    return this;
  }

  /**
   * Returns the documentation for this function.
   */
  public String getComment()
  {
    return null;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public ValueOrVar []evalArguments(Env env, Expr fun, Expr []args)
  {
    ValueOrVar[]values = new ValueOrVar[args.length];

    for (int i = 0; i < args.length; i++)
      values[i] = args[i].evalArg(env, VHelper.noCtx(), true).getOne();

    return values;
  }

  //
  // Value methods
  //

  //
  // Value predicates
  //

  /**
   * Returns true for an object
   */
  @Override
  public boolean isObject()
  {
    return true;
  }

  @Override
  public String getType()
  {
    return "object";
  }

  /**
   * The object is callable if it has an __invoke method
   */
  @Override
  public boolean isCallable(Env env, boolean isCheckSyntaxOnly, Value nameRef)
  {
    throw new UnimplementedException();
  }

  /**
   * Evaluates the function.
   */
  @Override
  abstract public V<? extends Value> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args);

  /**
   * Evaluates the function, returning a reference.
   */
  @Override
  public V<? extends Value> callRef(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args)
  {
    return call(env, ctx, args);
  }

  /**
   * Evaluates the function, returning a copy
   */
  @Override
  public V<? extends Value> callCopy(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args)
  {
    return call(env, ctx, args).map((a)->a.copyReturn());
  }

  /**
   * Evaluates the function as a closure.
   */
  public V<? extends Value> callClosure(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args, V<? extends ValueOrVar>[] useArgs)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Evaluates the function.
   */
  @Override
  public @NonNull V<? extends Value> call(Env env, FeatureExpr ctx)
  {
    return call(env, ctx, NULL_ARG_VALUES);
  }



  //
  // method calls
  //

  /**
   * Evaluates the method call.
   */
  public V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                                       QuercusClass qClass,
                                       Value qThis,
                                       V<? extends ValueOrVar>[] args)
  {
    throw new IllegalStateException(getClass().getName());

    /*
    Value oldThis = env.setThis(qThis);
    QuercusClass oldClass = env.setCallingClass(qClass);

    try {
      return call(env, ctx, args);
    } finally {
      env.setThis(oldThis);
      env.setCallingClass(oldClass);
    }
    */
  }

  /**
   * Evaluates the new() method call.
   */
  public @NonNull V<? extends Value> callNew(Env env,
                                    FeatureExpr ctx, QuercusClass qClass,
                                    Value qThis,
                                             V<? extends ValueOrVar>[] args)
  {
    return callMethod(env, ctx, qClass, qThis, args);
  }

  /**
   * Evaluates the method call, returning a reference.
   */
  public V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                                          QuercusClass qClass,
                                          Value qThis,
                                          V<? extends ValueOrVar>[] args)
  {
    throw new IllegalStateException(getClass().getName());

    /*
    Value oldThis = env.setThis(qThis);
    QuercusClass oldClass = env.setCallingClass(qClass);

    try {
      return callRef(env, ctx, args);
    } finally {
      env.setThis(oldThis);
      env.setCallingClass(oldClass);
    }
    */
  }

  /**
   * Evaluates the function as a method call.
   */
  public @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis)
  {
    return callMethod(env, ctx, qClass, qThis, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis)
  {
    return callMethodRef(env, ctx, qClass, qThis, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                                                      V<? extends ValueOrVar> a1)
  {
    return callMethod(env, ctx, qClass, qThis,
                      new V[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis,
                                                   V<? extends ValueOrVar> a1)
  {
    return callMethodRef(env, ctx, qClass, qThis,
                         new V[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                                                V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2)
  {
    return callMethod(env, ctx, qClass, qThis,
                      new V[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis,
                                                   V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2)
  {
    return callMethodRef(env, ctx, qClass, qThis,
                         new V[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                                                      V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3)
  {
    return callMethod(env, ctx, qClass, qThis,
                      new V[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis,
                                                         V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3)
  {
    return callMethodRef(env, ctx, qClass, qThis,
                         new V[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                                                      V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3, V<? extends ValueOrVar>a4)
  {
    return callMethod(env, ctx, qClass, qThis,
                      new V[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis,
                                                         V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3, V<? extends ValueOrVar>a4)
  {
    return callMethodRef(env, ctx, qClass, qThis,
                         new V[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                                                V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3, V<? extends ValueOrVar>a4, V<? extends ValueOrVar>a5)
  {
    return callMethod(env, ctx, qClass, qThis,
                      new V[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public final @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                             QuercusClass qClass,
                             Value qThis,
                                                   V<? extends ValueOrVar> a1, V<? extends ValueOrVar>a2, V<? extends ValueOrVar>a3, V<? extends ValueOrVar>a4, V<? extends ValueOrVar>a5)
  {
    return callMethodRef(env, ctx, qClass, qThis,
                         new V[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public  @NonNull V<? extends Value> callMethod(Env env, FeatureExpr ctx,
                          QuercusClass qClass,
                          Value qThis,
                          Expr []exprs)
  {
    V<? extends ValueOrVar> []argValues = new V[exprs.length];
    Arg []args = getArgs(env);

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference()) {
        argValues[i] = exprs[i].evalArg(env, ctx, true);
      }
      else
        argValues[i] = exprs[i].eval(env, ctx);
    }

    return callMethod(env, ctx, qClass, qThis, argValues);
  }

  /**
   * Evaluates the function.
   */
  public @NonNull V<? extends Value> callMethodRef(Env env, FeatureExpr ctx,
                                          QuercusClass qClass,
                                          Value qThis,
                                          Expr []exprs)
  {
    V<? extends ValueOrVar> []argValues = new V[exprs.length];
    Arg []args = getArgs(env);

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference())
        argValues[i] = exprs[i].evalArg(env, ctx, true);
      else
        argValues[i] = exprs[i].eval(env, ctx);
    }

    return callMethodRef(env, ctx, qClass, qThis, argValues);
  }

  protected Value errorProtectedAccess(Env env, Value oldThis)
  {
    return env.error(L.l(
      "Cannot call protected method {0}::{1}() from '{2}' context",
      getDeclaringClassName(),
      getName(),
      oldThis != null ? oldThis.getClassName() : null));
  }

  protected Value errorPrivateAccess(Env env, Value oldThis)
  {
    return env.error(L.l(
      "Cannot call private method {0}::{1}() from '{2}' context",
      getDeclaringClassName(),
      getName(),
      oldThis != null ? oldThis.getClassName() : null));
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}

