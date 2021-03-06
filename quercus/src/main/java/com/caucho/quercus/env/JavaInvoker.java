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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import edu.cmu.cs.varex.annotation.VParamType;
import edu.cmu.cs.varex.annotation.VSideeffectFree;
import edu.cmu.cs.varex.annotation.VVariational;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Represents the introspected static function information.
 */
@SuppressWarnings("serial")
abstract public class JavaInvoker
  extends AbstractJavaMethod
{
  private static final L10N L = new L10N(JavaInvoker.class);

  private static final V<? extends Value>[] NULL_VALUES = new V[0];

  private final ModuleContext _moduleContext;
  private final JavaClassDef _classDef;

  private final String _name;
  protected final Method _method;
  private final Class<?> [] _param;
  private final Class<?> _retType;
  private final Annotation [][] _paramAnn;
  private final Annotation []_methodAnn;

  private volatile boolean _isInit;

  private int _minArgumentLength;
  private int _maxArgumentLength;

  private boolean _hasEnv;
  private boolean _hasCtx;
  private boolean _hasThis;
  private Expr [] _defaultExprs;
  private Marshal []_marshalArgs;
  private boolean _hasRestArgs;
  private Marshal _unmarshalReturn;

  private boolean _isRestReference;

  private boolean _isCallUsesVariableArgs;
  private boolean _isCallUsesSymbolTable;
  private boolean _vsideeffectFree = false;
  private boolean _vvariational = false;

  /**
   * Creates the statically introspected function.
   */
  public JavaInvoker(ModuleContext moduleContext,
                     JavaClassDef classDef,
                     Method method,
                     String name,
                     Class<?> []param,
                     Annotation [][]paramAnn,
                     Annotation []methodAnn,
                     Class<?> retType)
  {
    _moduleContext = moduleContext;
    _name = name;
    _param = param;
    _paramAnn = paramAnn;
    _methodAnn = methodAnn;
    _retType = retType;

    _classDef = classDef;
    _method = method;

    // init();
  }

  /**
   * Creates the statically introspected function.
   */
  public JavaInvoker(ModuleContext moduleContext,
                     JavaClassDef classDef,
                     Method method)
  {
    _name = getFunctionName(method);

    _moduleContext = moduleContext;
    _classDef = classDef;
    _method = method;
    _param = method.getParameterTypes();
    _paramAnn = null;
    _methodAnn = null;
    _retType = method.getReturnType();

    // init();
  }

  public static String getFunctionName(Method method)
  {
    Name nameAnn = method.getAnnotation(Name.class);

    if (nameAnn != null)
      return nameAnn.value();
    else
      return method.getName();
  }

  public void init()
  {
    if (_isInit)
      return;

    synchronized (this) {
      if (_isInit)
        return;

      if (_method != null) {
        // php/069a
        // Java 6 fixes the need to do this for methods of inner classes
        _method.setAccessible(true);
      }

      MarshalFactory marshalFactory = _moduleContext.getMarshalFactory();
      ExprFactory exprFactory = _moduleContext.getExprFactory();

      Annotation [][]paramAnn = getParamAnnImpl();
      Annotation []methodAnn = getMethodAnn();

      try {
        boolean callUsesVariableArgs = false;
        boolean callUsesSymbolTable = false;
        boolean returnNullAsFalse = false;
        Class<?> vretType = null;

        for (Annotation ann : methodAnn) {
          if (VariableArguments.class.isAssignableFrom(ann.annotationType()))
            callUsesVariableArgs = true;

          if (UsesSymbolTable.class.isAssignableFrom(ann.annotationType()))
            callUsesSymbolTable = true;

          if (ReturnNullAsFalse.class.isAssignableFrom(ann.annotationType()))
            returnNullAsFalse = true;

          if (VSideeffectFree.class.isAssignableFrom(ann.annotationType()))
            _vsideeffectFree = true;

          if (VVariational.class.isAssignableFrom(ann.annotationType()))
            _vvariational = true;

          if (VParamType.class.isAssignableFrom(ann.annotationType()))
            vretType = ((VParamType)ann).value();
        }

        _isCallUsesVariableArgs = callUsesVariableArgs;
        _isCallUsesSymbolTable = callUsesSymbolTable;

        _hasEnv = _param.length > 0 && _param[0].equals(Env.class);
        int envOffset = _hasEnv ? 1 : 0;
        _hasCtx = envOffset < _param.length && _param[envOffset].equals(FeatureExpr.class);
        if (_hasCtx)
          envOffset++;

        if (envOffset < _param.length)
          _hasThis = hasThis(_param[envOffset], paramAnn[envOffset]);
        else
          _hasThis = false;

        if (_hasThis)
          envOffset++;

        boolean hasRestArgs = false;
        boolean isRestReference = false;

        if (_param.length > 0
            && (_param[_param.length - 1].equals(Value[].class)
                || _param[_param.length - 1].equals(Object[].class)
                || _param[_param.length - 1].equals(V[].class))) {
          hasRestArgs = true;

          for (Annotation ann : paramAnn[_param.length - 1]) {
            if (Reference.class.isAssignableFrom(ann.annotationType()))
              isRestReference = true;
          }
        }

        _hasRestArgs = hasRestArgs;
        _isRestReference = isRestReference;

        int argLength = _param.length;

        if (_hasRestArgs)
          argLength -= 1;

        _defaultExprs = new Expr[argLength - envOffset];
        _marshalArgs = new Marshal[argLength - envOffset];

        _maxArgumentLength = argLength - envOffset;
        _minArgumentLength = _maxArgumentLength;

        for (int i = 0; i < argLength - envOffset; i++) {
          boolean isOptional = false;
          boolean isReference = false;
          boolean isPassThru = false;

          boolean isNotNull = false;

          boolean isExpectString = false;
          boolean isExpectNumeric = false;
          boolean isExpectBoolean = false;

          boolean isVariational = false;

          Class<?> argType = _param[i + envOffset];
          Class<?> vargType = null;

          for (Annotation ann : paramAnn[i + envOffset]) {
            if (Optional.class.isAssignableFrom(ann.annotationType())) {
              _minArgumentLength--;
              isOptional = true;

              Optional opt = (Optional) ann;

              if (opt.value().equals(Optional.NOT_SET))
                _defaultExprs[i] = exprFactory.createDefault();
              else if (opt.value().equals("")) {
                _defaultExprs[i] = exprFactory.createLiteral(StringValue.EMPTY);
              }
              else {
                _defaultExprs[i] = QuercusParser.parseDefault(exprFactory,
                                                              opt.value());
              }
            } else if (Reference.class.isAssignableFrom(ann.annotationType())) {
              if (! Var.class.equals(argType) && ! Var[].class.equals(argType)) {
                throw new QuercusException(
                  L.l("reference must be Var for {0} in {1}", _name, _method));
              }

              isReference = true;
            } else if (PassThru.class.isAssignableFrom(ann.annotationType())) {
              if (! Value.class.equals(argType)) {
                throw new QuercusException(
                  L.l("pass thru must be Value for {0}", _name));
              }

              isPassThru = true;
            } else if (NotNull.class.isAssignableFrom(ann.annotationType())) {
              isNotNull = true;
            } else if (Expect.class.isAssignableFrom(ann.annotationType())) {
              if (! Value.class.equals(argType)) {
                throw new QuercusException(L.l(
                  "Expect type must be Value for {0}",
                  _name));
              }

              Expect.Type type = ((Expect) ann).type();

              if (type == Expect.Type.STRING) {
                isExpectString = true;
              }
              else if (type == Expect.Type.NUMERIC) {
                isExpectNumeric = true;
              }
              else if (type == Expect.Type.BOOLEAN) {
                isExpectBoolean = true;
              }
            } else if (VParamType.class.isAssignableFrom(ann.annotationType())) {
              vargType = ((VParamType) ann).value();
            }

          }
          isVariational = V.class.isAssignableFrom(argType);
          if (_vvariational && !isVariational)
            throw new QuercusException(L.l(
                    "Expect variational type for {0}, parameter {1}",
                    _name, i));
          if (!_vvariational && isVariational)
            throw new QuercusException(L.l(
                    "Unexpected variational type for {0}",
                    _name));
          if (isVariational && vargType == null)
            throw new QuercusException(L.l(
                    "Variational parameter without @VParamType annotation not supported for {0}",
                    _name));
          if (isVariational && vargType != null)
            argType=vargType;

          if (isReference) {
            _marshalArgs[i] = marshalFactory.createReference();
          }
          else if (isPassThru) {
            _marshalArgs[i] = marshalFactory.createValuePassThru();
          }
          else if (isExpectString) {
            _marshalArgs[i] = marshalFactory.createExpectString();
          }
          else if (isExpectNumeric) {
            _marshalArgs[i] = marshalFactory.createExpectNumeric();
          }
          else if (isExpectBoolean) {
            _marshalArgs[i] = marshalFactory.createExpectBoolean();
          }
          else {
            _marshalArgs[i] = marshalFactory.create(argType,
                                                    isNotNull,
                                                    false,
                                                    isOptional);
          }
        }

        Class<?> retType = _retType;
        if (_vvariational && !V.class.isAssignableFrom(_retType))
          throw new QuercusException(L.l(
                  "Expect variational return type for {0}",
                  _name));
        if (!_vvariational && V.class.isAssignableFrom(_retType))
          throw new QuercusException(L.l(
                  "Unexpected variational return type for {0} in {1}",
                  _name,_method));
        if (_vvariational) {
          if (vretType==null)
            throw new QuercusException(L.l(
                    "Variational method without @VParamType annotation for its return type not supported for {0}",
                    _name));
          retType=vretType;
        }


        _unmarshalReturn = marshalFactory.create(retType,
                                                 false,
                                                 returnNullAsFalse,
                                                 false);
      } finally {
        _isInit = true;
      }
    }
  }

  /**
   * Returns the implementing class.
   */
  @Override
  public ClassDef getDeclaringClass()
  {
    return _classDef;
  }

  /**
   * Returns the minimally required number of arguments.
   */
  @Override
  public int getMinArgLength()
  {
    if (! _isInit)
      init();

    return _minArgumentLength;
  }

  /**
   * Returns the maximum number of arguments allowed.
   */
  @Override
  public int getMaxArgLength()
  {
    if (! _isInit)
      init();

    return _maxArgumentLength;
  }

  @Override
  public Class<?> getJavaDeclaringClass()
  {
    if (_method != null) {
      return _method.getDeclaringClass();
    }
    else {
      return null;
    }
  }

  /**
   * Returns true if the environment is an argument.
   */
  public boolean getHasEnv()
  {
    if (! _isInit)
      init();

    return _hasEnv;
  }

  /**
   * Returns true if the environment has rest-style arguments.
   */
  @Override
  public boolean getHasRestArgs()
  {
    if (! _isInit)
      init();

    return _hasRestArgs;
  }

  /**
   * Returns true if the rest argument is a reference.
   */
  public boolean isRestReference()
  {
    if (! _isInit)
      init();

    return _isRestReference;
  }

  /**
   * Returns the unmarshaller for the return
   */
  public Marshal getUnmarshalReturn()
  {
    if (! _isInit)
      init();

    return _unmarshalReturn;
  }

  /**
   * Returns true if the call uses variable arguments.
   */
  @Override
  public boolean isCallUsesVariableArgs()
  {
    if (! _isInit)
      init();

    return _isCallUsesVariableArgs;
  }

  /**
   * Returns true if the call uses the symbol table
   */
  @Override
  public boolean isCallUsesSymbolTable()
  {
    if (! _isInit)
      init();

    return _isCallUsesSymbolTable;
  }

  /**
   * Returns true if the result is a boolean.
   */
  @Override
  public boolean isBoolean()
  {
    if (! _isInit)
      init();

    return _unmarshalReturn.isBoolean();
  }

  /**
   * Returns true if the result is a string.
   */
  @Override
  public boolean isString()
  {
    if (! _isInit)
      init();

    return _unmarshalReturn.isString();
  }

  /**
   * Returns true if the result is a long.
   */
  @Override
  public boolean isLong()
  {
    if (! _isInit)
      init();

    return _unmarshalReturn.isLong();
  }

  /**
   * Returns true if the result is a double.
   */
  @Override
  public boolean isDouble()
  {
    if (! _isInit)
      init();

    return _unmarshalReturn.isDouble();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the marshal arguments.
   */
  public Marshal []getMarshalArgs()
  {
    if (! _isInit)
      init();

    return _marshalArgs;
  }

  /**
   * Returns the parameter annotations.
   */
  protected Annotation [][]getParamAnn()
  {
    if (! _isInit)
      init();

    return getParamAnnImpl();
  }

  private Annotation [][]getParamAnnImpl()
  {
    if (_paramAnn != null)
      return _paramAnn;
    else
      return _method.getParameterAnnotations();
  }

  /**
   * Returns the parameter annotations.
   */
  protected Annotation []getMethodAnn()
  {
    if (_methodAnn != null)
      return _methodAnn;
    else
      return _method.getAnnotations();
  }

  /**
   * Returns the default expressions.
   */
  protected Expr []getDefaultExprs()
  {
    if (! _isInit)
      init();

    return _defaultExprs;
  }

  /**
   * Evaluates a function's argument, handling ref vs non-ref
   */
  @Override
  public Value []evalArguments(Env env, Expr fun, Expr []args)
  {
    if (! _isInit)
      init();

    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Marshal arg = null;

      if (i < _marshalArgs.length)
        arg = _marshalArgs[i];
      else if (_isRestReference) {
        values[i] = args[i].evalVar(env, VHelper.noCtx()).getOne().makeValue();
        continue;
      }
      else {
        values[i] = args[i].eval(env, VHelper.noCtx()).getOne();
        continue;
      }

      if (arg == null)
        values[i] = args[i].eval(env, VHelper.noCtx()).getOne().copy();
      else if (arg.isReference())
        values[i] = args[i].evalRef(env, VHelper.noCtx()).getOne().toValue();
      else {
        // php/0d04
        values[i] = args[i].eval(env, VHelper.noCtx()).getOne();
      }
    }

    return values;
  }

  /**
   * Returns the cost of marshaling for this method.
   */
  @Override
  public int getMarshalingCost(Value []args)
  {
    if (! _isInit)
      init();

    if (_hasRestArgs) {
    }
    else if (args.length < getMinArgLength()) {
      // not enough args
      return Integer.MAX_VALUE;
    }
    else if (args.length > getMaxArgLength()) {
      // too many args
      return Integer.MAX_VALUE;
    }

    int cost = 0;
    int i = 0;

    for (; i < _marshalArgs.length; i++) {
      Marshal marshal = _marshalArgs[i];

      if (i < args.length && args[i] != null) {
        Value arg = args[i].toValue();

        int argCost = marshal.getMarshalingCost(arg);

        cost = Math.max(argCost + cost, cost);
      }
    }

    // consume all the REST args
    if (_hasRestArgs) {
      int restLen = args.length - _marshalArgs.length;

      if (restLen > 0)
        i += restLen;
    }

    // too many args passed in
    if (i > getMaxArgLength()) {
      return Integer.MAX_VALUE;
    }

    return cost;
  }

  @Override
  public int getMarshalingCost(Expr []args)
  {
    if (! _isInit)
      init();

    if (_hasRestArgs) {
    }
    else if (args.length < getMinArgLength()) {
      // not enough args
      return Integer.MAX_VALUE;
    }
    else if (args.length > getMaxArgLength()) {
      // too many args
      return Integer.MAX_VALUE;
    }

    int cost = 0;
    int i = 0;

    for (; i < _marshalArgs.length; i++) {
      Marshal marshal = _marshalArgs[i];

      if (i < args.length && args[i] != null) {
        Expr arg = args[i];

        int argCost = marshal.getMarshalingCost(arg);

        cost = Math.max(argCost + cost, cost);
      }
    }

    // consume all the REST args
    if (_hasRestArgs) {
      int restLen = args.length - _marshalArgs.length;

      if (restLen > 0)
        i += restLen;
    }

    // too many args passed in
    if (i > getMaxArgLength())
      return Integer.MAX_VALUE;

    return cost;
  }

  @Override
  public V<? extends ValueOrVar> call(Env env, FeatureExpr ctx, V<? extends ValueOrVar>[] args)
  {
    return callMethod(env, ctx, (QuercusClass) null, (Value) null, args);
  }

  @Override
  public V<? extends ValueOrVar> callMethodRef(Env env, FeatureExpr ctx,
                                               QuercusClass qClass,
                                               Value qThis,
                                               V<? extends ValueOrVar>[] args)
  {
    // php/3cl3
    return callMethod(env, ctx, qClass, qThis, args);
  }

  @Override
  public V<? extends ValueOrVar> callMethod(Env env, FeatureExpr ctx,
                                            QuercusClass qClass,
                                            Value qThis,
                                            V<? extends ValueOrVar>[] args)
  {
    V<? extends Object> result = callJavaMethod(env, ctx, qClass, qThis, args);

    // php/0k45
    if (qThis != null && isConstructor()) {
      String parentName = qThis.getQuercusClass().getParentName();

      if (parentName != null) {
        ClassDef classDef = getDeclaringClass();

        if (classDef != null && qThis.isA(env, classDef.getName())) {
          qThis.setJavaObject(result);     //TODO V
        }
      }
    }

    V<? extends Value> value = result.smap(ctx, (c, a) -> _unmarshalReturn.unmarshal(env, c, a));

    return value;
  }

  @Override
  public @Nonnull
  V<? extends ValueOrVar> callNew(Env env,
                                  FeatureExpr ctx, QuercusClass qClass,
                                  Value qThis,
                                  V<? extends ValueOrVar>[] args)
  {
    Object result = callJavaMethod(env, ctx, qClass, qThis, args);

    qThis.setJavaObject(result);

    return VHelper.toV(qThis);
  }

  private V<? extends Object> callJavaMethod(Env env,
                                             FeatureExpr ctx, QuercusClass qClass,
                                             Value qThis,
                                             V<? extends ValueOrVar>[] args)
  {
    if (! _isInit) {
      init();
    }

    int len = _param.length;

    Object []javaArgs = new Object[len];

    int k = 0;

    if (_hasEnv)
      javaArgs[k++] = env;

    if (_hasCtx)
      javaArgs[k++] = ctx;

    Object obj = null;

    if (_hasThis) {
      obj = qThis != null ? qThis.toJavaObject() : null;
      javaArgs[k++] = qThis;
    }
    else if (! isStatic() && ! isConstructor()) {
      obj = qThis != null ? qThis.toJavaObject() : null;
    }

    int vParamStartAt = k;

    String warnMessage = null;
    for (int i = 0; i < _marshalArgs.length; i++) {
      int _i = i, _k = k;
      if (i < args.length && args[i] != null)
        javaArgs[k] = _marshalArgs[_i].marshal(env, ctx, args[i], _param[_k]);
      else if (_defaultExprs[i] != null) {
        javaArgs[k] =
                _marshalArgs[_i].marshal(env,
                        ctx, _defaultExprs[i].eval(env, ctx),
                        _param[_k]);
      } else {
        warnMessage = L.l(
          "function '{0}' has {1} required arguments, "
          + "but only {2} were provided",
          _name,
          _minArgumentLength,
          args.length);

        //return NullValue.NULL;

        javaArgs[k] = V.one(_marshalArgs[i].marshal(env, ctx, V.one(NullValue.NULL), _param[k]));
      }

      /*
      if (javaArgs[k] != null)
        System.out.println("ARG: " + javaArgs[k] + " " + _marshalArgs[i]);
      */

      k++;
    }

    if (warnMessage != null)
      env.warning(warnMessage);

    if (_hasRestArgs) {
      V<? extends Value>[] rest;

      int restLen = args.length - _marshalArgs.length;

      if (restLen <= 0)
        rest = NULL_VALUES;
      else {
        rest = new V[restLen];

        for (int i = _marshalArgs.length; i < args.length; i++) {
          if (_isRestReference) {
            rest[i - _marshalArgs.length] = args[i].flatMap(a->a.toLocalVarDeclAsRef()).map(a->a.makeValue());
          }
          else
            rest[i - _marshalArgs.length] = args[i].flatMap(a->a._getValues());
        }
      }

      javaArgs[k++] = rest;
    }
    else if (_marshalArgs.length < args.length) {
      // php/153o
      env.warning(L.l(
        "function '{0}' called with {1} arguments, "
        + "but only expects {2} arguments",
        _name,
        args.length,
        _marshalArgs.length));
    }

    if (_vvariational)
      return (V<? extends Object>) invoke(obj, javaArgs);
    else if (_vsideeffectFree)
      return invokeBruteForce(ctx, obj, vParamStartAt, javaArgs);
    else {
      //calling only a single time, hoping that none of the parameters are variational
      try {
        for (int i = vParamStartAt; i < javaArgs.length; i++) {
          if (javaArgs[i] instanceof V)
            javaArgs[i] = ((V<? extends Value>) javaArgs[i]).getOne(ctx);
          else if (javaArgs[i] instanceof V[])
            javaArgs[i] = varrayToArray(ctx, _param[i], (V[]) javaArgs[i]);
          else throw new QuercusException(L.l(
                    "Unexpected parameter {1} in \"{0}\"",
                    _method, i));
        }
      } catch (AssertionError e) {
        throw new QuercusException(L.l(
                "Call to unlifted library function \"{0}\" with variational parameter. {1}",
                _method, e.getMessage()));
      }
      return V.one(invoke(obj, javaArgs));
    }
  }

  private static <T> T[] varrayToArray(FeatureExpr ctx, Class<T> c, V<? extends T>[] a) {
    T[] result = (T[]) Array.newInstance(c.getComponentType(), a.length);
    for (int i = 0; i < a.length; i++)
      result[i] = a[i].getOne(ctx);
    return result;
  }

  private V<? extends Object> invokeBruteForce(FeatureExpr ctx, Object obj, int vParamStartAt, Object[] args) {
    //turn array of variational parameter into choice of plain arrays (skip the first vParamStartAt entries)

    V<? extends Object[]> plainArgs = bruteForceArgs(ctx, args, vParamStartAt);

    return plainArgs.smap(ctx, a -> invoke(obj, a));
  }

  @SuppressWarnings("RedundantCast")
  private V<? extends Object[]> bruteForceArgs(FeatureExpr ctx, Object[] input, int idx) {
    if (idx >= input.length)
      return V.one(ctx, input);

    return bruteForceArgs(ctx, input, idx + 1).<Object[]>sflatMap(ctx, vparams -> {
      V<?> vparam = (V<?>) ((Object[]) vparams)[idx];
      return vparam.<Object[]>map(param -> {
        Object[] result = ((Object[]) vparams).clone();
        result[idx] = param;
        return result;
      });
    });
  }


  abstract public Object invoke(Object obj, Object []args);

  //
  // Utility methods
  //
  private boolean hasThis(Class<?> param, Annotation[]ann)
  {
    if (! param.isAssignableFrom(ObjectValue.class))
      return false;

    for (int i = 0; i < ann.length; i++) {
      if (This.class.isAssignableFrom(ann[i].annotationType()))
        return true;
    }

    return false;
  }
}
