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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.*;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.StderrStream;
import com.caucho.vfs.StringWriter;
import com.caucho.vfs.WriteStream;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VHelper;
import edu.cmu.cs.varex.VWriteStream;
import edu.cmu.cs.varex.annotation.VParamType;
import edu.cmu.cs.varex.annotation.VVariational;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Information about PHP variables.
 */
public class VariableModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(VariableModule.class.getName());
  private static final L10N L = new L10N(VariableModule.class);

  private static final
    LruCache<UnserializeKey,UnserializeCacheEntry> _unserializeCache
    = new LruCache<UnserializeKey,UnserializeCacheEntry>(256);

  /**
   * Returns a constant
   *
   * @param env the quercus calling environment
   * @param name the constant name
   */
  public static Value constant(Env env, String name)
  {
    if (name == null) {
      env.warning(L.l("null passed as constant name"));
      return NullValue.NULL;
    }

    int i = name.indexOf("::");

    if (i > 0) {
      String cls = name.substring(0, i);

      name = name.substring(i + 2);

      return env.getClass(cls).getConstant(env, env.createString(name));
    }
    else {
      Value constant = env.getConstant(name, false);

      if (constant != null)
        return constant;
      else {
        env.warning(L.l("cannot find constant '{0}'", name));
        return NullValue.NULL;
      }
    }
  }

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value debug_zval_dump(Env env, @ReadOnly Value v)
  {
    try {
      debug_impl(env, v, 0);

      return NullValue.NULL;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Defines a constant
   *
   * @param env the quercus calling environment
   * @param name the constant name
   * @param value the constant value
   */
  public static Value define(Env env,
                             StringValue name,
                             Value value,
                             @Optional boolean isCaseInsensitive)
  {
    return env.addConstant(name, value, isCaseInsensitive);
  }

  /**
   * Returns true if the constant is defined.
   *
   * @param env the quercus calling environment
   * @param name the constant name
   */
  public static boolean defined(Env env, String name)
  {
    if (name == null)
      return false;

    int i = name.indexOf("::");

    if (i > 0) {
      String clsName = name.substring(0, i);
      name = name.substring(i + 2);

      QuercusClass cls = env.getClass(clsName);

      return cls.hasConstant(env.createString(name));
    }
    else
      return env.isDefined(name);
  }

  /**
   * Converts to a double
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value doubleval(@ReadOnly Value v)
  {
    return floatval(v);
  }

  /**
   * Returns true for an empty variable.
   *
   * @param v the value to test
   *
   * @return true if the value is empty
   */
  public static boolean empty(@ReadOnly Value v)
  {
    return v.isEmpty().getOne();
  }

  /**
   * Converts to a double
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value floatval(@ReadOnly Value v)
  {
    return new DoubleValue(v.toDouble());
  }

  /**
   * Returns the defined variables in the current scope.
   */
  @UsesSymbolTable
  public static Value get_defined_vars(Env env)
  {
    ArrayValue result = new ArrayValueImpl();

    Map<StringValue,EnvVar> map = env.getEnv();

    for (Map.Entry<StringValue,EnvVar> entry : map.entrySet()) {
      result.append(entry.getKey(),
                    entry.getValue().getOne());
    }

    Map<StringValue,EnvVar> globalMap = env.getGlobalEnv();
    if (map != globalMap) {
      for (Map.Entry<StringValue,EnvVar> entry : globalMap.entrySet()) {
        result.append(entry.getKey(),
                      entry.getValue().getOne());
      }
    }

    return result;
  }

  /*
   * Returns the type of this resource.
   */
  @ReturnNullAsFalse
  public static String get_resource_type(Env env, Value v)
  {
    return v.getResourceType();
  }

  /**
   * Returns the type string for the variable
   */
  public static String gettype(@ReadOnly Value v)
  {
    return v.getType();
  }

  /**
   * Imports request variables
   *
   * @param types the variables to import
   * @param prefix the prefix
   */
  public static boolean import_request_variables(Env env,
                                                 String types,
                                                 @Optional String prefix)
  {
    if ("".equals(prefix))
      env.notice(L.l("import_request_variables should use a prefix argument"));

    for (int i = 0; i < types.length(); i++) {
      char ch = types.charAt(i);

      Value value = null;

      if (ch == 'c' || ch == 'C')
        value = env.getGlobalValue(VHelper.noCtx(), "_COOKIE").getOne();
      else if (ch == 'g' || ch == 'G')
        value = env.getGlobalValue(VHelper.noCtx(), "_GET").getOne();
      else if (ch == 'p' || ch == 'P')
        value = env.getGlobalValue(VHelper.noCtx(), "_POST").getOne();

      if (! (value instanceof ArrayValue))
        continue;

      ArrayValue array = (ArrayValue) value;

      for (VEntry entry : array.entrySet()) {
        String key = entry.getKey().toString();

        env.setGlobalValue(VHelper.noCtx(), prefix + key,
                         array.getVar(VHelper.noCtx(), entry.getKey()).getValue());
      }
    }

    return true;
  }

  /**
   * Converts to a long
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value intval(@ReadOnly Value v)
  {
    return v.toLongValue();
  }

  /**
   * Converts to a long
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static long intval(@ReadOnly Value v, int base)
  {
    if (! v.isString())
      return v.toLong();

    StringValue s = v.toStringValue();

    int len = s.length();
    long value = 0;

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      int digit;

      if ('0' <= ch && ch <= '9')
        digit = ch - '0';
      else if ('a' <= ch && ch <= 'z')
        digit = ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'Z')
        digit = ch - 'A' + 10;
      else
        digit = 0;

      value = value * base + digit;
    }

    return value;
  }

  /**
   * Returns true for an array.
   *
   * @param v the value to test
   *
   * @return true for an array
   */
  public static boolean is_array(@ReadOnly Value v)
  {
    return v.isArray();
  }

  // XXX: is_binary

  /**
   * Returns true for a boolean
   *
   * @param v the value to test
   *
   * @return true for a boolean
   */
  public static Value is_bool(@ReadOnly Value v)
  {
    return (v.toValue() instanceof BooleanValue
            ? BooleanValue.TRUE
            : BooleanValue.FALSE);
  }

  // XXX: is_buffer

  /**
   * Returns the type string for the variable
   */
  public static boolean is_callable(Env env,
                                    @ReadOnly Value v,
                                    @Optional boolean isCheckSyntaxOnly,
                                    @Optional @Reference Var nameRef)
  {
    if (nameRef.makeValue().isDefault()) {
      nameRef = null;
    }

    return v.isCallable(env, isCheckSyntaxOnly, nameRef);
  }

  /**
   * Returns true for a double
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static boolean is_double(@ReadOnly Value v)
  {
    return is_float(v);
  }

  /**
   * Returns true for a double
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static boolean is_float(@ReadOnly Value v)
  {
    return (v.toValue() instanceof DoubleValue);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_int(@ReadOnly Value v)
  {
    return (v.toValue() instanceof LongValue
            ? BooleanValue.TRUE
            : BooleanValue.FALSE);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_integer(@ReadOnly Value v)
  {
    return is_int(v);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_long(@ReadOnly Value v)
  {
    return is_int(v);
  }

  /**
   * Returns true for null
   *
   * @param v the value to test
   *
   * @return true for null
   */
  public static boolean is_null(@ReadOnly Value v)
  {
    return v.isNull();
  }

  /**
   * Returns true for numeric
   *
   * @param env the calling environment
   * @param v the value to test
   *
   * @return true for numeric
   */
  public static boolean is_numeric(Env env, @ReadOnly Value v)
  {
    return v.isNumeric();
  }

  /**
   * Returns true for an object
   *
   * @param env the calling environment
   * @param v the value to test
   *
   * @return true for object
   */
  public static boolean is_object(Env env, @ReadOnly Value v)
  {
    return v.isObject();
  }

  /**
   * Returns true for a real
   *
   * @param v the value to test
   *
   * @return true for a real
   */
  public static boolean is_real(@ReadOnly Value v)
  {
    return is_float(v);
  }

  /**
   * Returns true if the value is a resource
   */
  public boolean is_resource(@ReadOnly Value value)
  {
    return value.isResource();
  }

  /**
   * Returns true for a scalar
   *
   * @param v the value to test
   *
   * @return true for a scalar
   */
  public static boolean is_scalar(@ReadOnly Value v)
  {
    if (v == null) {
      return false;
    }

    Value value = v.toValue();

    return (value instanceof DoubleValue
            || value instanceof StringValue
            || value instanceof LongValue
            || value instanceof BooleanValue);
  }

  /**
   * Returns true if the value is a string
   */
  public boolean is_string(@ReadOnly Value value)
  {
    return value.isString();
  }

  // XXX: is_unicode

  /**
   * Returns the type string for the variable
   */
  public static boolean isset(@ReadOnly Value ... values)
  {
    for (Value v : values) {
      if (! v.isset()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Prints a value.  If isReturn is true, then returns what was supposed
   * to be printed as a string instead.
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @param isReturn set to true if returning instead of printing value
   * @return the string that was supposed to be printed, or true
   */
  @VVariational
  @VParamType(Value.class)
  public static V<? extends Value> print_r(Env env, FeatureExpr ctx,
                                           @VParamType(Value.class) @ReadOnly V<? extends Value> v,
                                           @VParamType(Boolean.class) @Optional V<? extends Boolean> isReturn) {
    VWriteStream out;

    if (isReturn.getOne(ctx) == Boolean.TRUE) {
      //TODO lift completely to support also conditional output within the string and lift createString + writer.getString()
      StringWriter writer = new StringWriter();
      out = VWriteStream.adapt(writer.openWrite());

      out.setNewlineString("\n");

      v.sforeach(ctx, (c, vv) -> vv.printR(env, c, out, 0, new IdentityHashMap<Value, String>()));

      return V.one(ctx, env.createString(writer.getString()));
    } else {
      out = env.getOut();

      v.sforeach(ctx, (c, vv) -> vv.printR(env, c, out, 0, new IdentityHashMap<Value, String>()));

      return V.one(ctx, BooleanValue.TRUE);
    }
  }

  private static void printDepth(VWriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++) {
      out.print(VHelper.noCtx(), ' ');
    }
  }

  /**
   * Serializes the value to a string.
   */
  public static String serialize(Env env,
                                 @PassThru @ReadOnly Value v)
  {
    StringBuilder sb = new StringBuilder();

    v.serialize(env, sb, new SerializeMap());

    return sb.toString();
  }

  /**
   * Converts the variable to a specified tyep.
   */
  public static boolean settype(Env env,
                                @Reference Var var,
                                String type)
  {
    Value value = var.toValue();

    if ("null".equals(type)) {
      var.set(NullValue.NULL);
      return true;
    }
    else if ("boolean".equals(type) || "bool".equals(type)) {
      var.set(value.toBoolean() ? BooleanValue.TRUE : BooleanValue.FALSE);
      return true;
    }
    else if ("string".equals(type)) {
      var.set(value.toStringValue());
      return true;
    }
    else if ("int".equals(type) || "integer".equals(type)) {
      var.set(LongValue.create(value.toLong()));
      return true;
    }
    else if ("float".equals(type) || "double".equals(type)) {
      var.set(new DoubleValue(value.toDouble()));
      return true;
    }
    else if ("object".equals(type)) {
      var.set(value.toObject(env));
      return true;
    }
    else if ("array".equals(type)) {
      if (value.isArray())
        var.set(value);
      else {
        ArrayValueImpl array = new ArrayValueImpl();
        var.set(array);

        if (! value.isNull()) {
          array.append(value);
        }
      }

      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Converts to a string
   *
   * @param env the quercus calling environment
   * @param v the variable to convert
   * @return the double value
   */
  public static Value strval(Env env, @ReadOnly Value v)
  {
    return v.toString(env);
  }

  /**
   * Unserializes the value from a string.
   */
  public static Value unserialize(Env env, StringValue s)
  {
    if (s.length() == 0) {
      return BooleanValue.FALSE;
    }

    Value v = null;

    UnserializeKey key = new UnserializeKey(s);

    UnserializeCacheEntry entry = _unserializeCache.get(key);

    if (entry != null) {
      v = entry.getValue(env);

      if (v != null) {
        return v;
      }
    }

    UnserializeReader is = null;

    try {
      is = new UnserializeReader(s);

      v = is.unserialize(env);
    }
    catch (IOException e) {
      System.err.println("VariableModule.unserialize0: " + s);

      env.notice(e);

      v = BooleanValue.FALSE;
    }

    if (is != null && ! is.useReference()) {
      entry = new UnserializeCacheEntry(v);

      _unserializeCache.put(key, entry);

      return entry.getValue(env);
    }

    return v;
  }

  // XXX: unset

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value var_dump(Env env, FeatureExpr ctx,
                               @PassThru @ReadOnly Value v,
                               Value []args)
  {
    try {
      if (v == null)
        env.getOut().print(ctx, "NULL#java");
      else {
        v.varDump(env, ctx, env.getOut(), 0, new IdentityHashMap<Value, String>());

        env.getOut().println(ctx);
      }

      if (args != null) {
        for (Value value : args) {
          if (value == null)
            env.getOut().print(ctx, "NULL#java");
          else {
            value.varDump(env, ctx, env.getOut(), 0,
                          new IdentityHashMap<Value,String>());

            env.getOut().println(ctx);
          }
        }
      }

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  public static Value stderr_var_dump(Env env,
                                      @PassThru @ReadOnly Value v,
                                      Value []args)
  {
    VWriteStream out = VWriteStream.adapt(new WriteStream(StderrStream.create()));

    try {
      if (v == null)
        out.print(VHelper.noCtx(), "NULL#java");
      else {
        v.varDump(env, VHelper.noCtx(), out, 0, new IdentityHashMap<Value, String>());

        out.println(VHelper.noCtx());
      }

      if (args != null) {
        for (Value value : args) {
          if (value == null)
            out.print(VHelper.noCtx(), "NULL#java");
          else {
            value.varDump(env, VHelper.noCtx(), out, 0,
                          new IdentityHashMap<Value,String>());

            out.println(VHelper.noCtx());
          }
        }
      }

      out.flush();

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Serializes the value to a string.
   */
  public static Value var_export(Env env,
                                 @ReadOnly Value v,
                                 @Optional boolean isReturn)
  {
    StringValue s = v.varExport(env);

    if (isReturn) {
      return s;
    }
    else {
      env.print(VHelper.noCtx(),s);

      return NullValue.NULL;
    }
  }

  private static void debug_impl(Env env, Value v, int depth)
    throws IOException
  {
    VWriteStream out = env.getOut();

    //TODO V
//    if (v instanceof Var)
//      out.print(VHelper.noCtx(), "&");

    v = v.toValue();

    if (v instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) v;

      out.println(VHelper.noCtx(), "Array");
      printDepth(out, 2 * depth);
      out.println(VHelper.noCtx(), "(");

      for (VEntry entry : array.entrySet()) {
        printDepth(out, 2 * depth);
        out.print(VHelper.noCtx(), "    [");
        out.print(VHelper.noCtx(), entry.getKey());
        out.print(VHelper.noCtx(), "] => ");
        debug_impl(env, entry.getEnvVar().getOne(), depth + 1); // XXX: recursion
      }
      printDepth(out, 2 * depth);
      out.println(VHelper.noCtx(), ")");
    }
    else if (v instanceof BooleanValue) {
      if (v.toBoolean())
        out.print(VHelper.noCtx(), "bool(true)");
      else
        out.print(VHelper.noCtx(), "bool(false)");
    }
    else if (v instanceof LongValue) {
      out.print(VHelper.noCtx(), "int(" + v.toLong() + ")");
    }
    else if (v instanceof DoubleValue) {
      out.print(VHelper.noCtx(), "float(" + v.toDouble() + ")");
    }
    else if (v instanceof StringValue) {
      out.print(VHelper.noCtx(), "string(" + v.toString() + ")");
    }
    else if (v instanceof NullValue) {
      out.print(VHelper.noCtx(), "NULL");
    }
    else {
      v.print(env, VHelper.noCtx());
    }
  }

  static class UnserializeKey {
    private final SoftReference<StringValue> _stringRef;
    private int _hash;

    UnserializeKey(StringValue string)
    {
      _hash = string.hashCode();

      _stringRef = new SoftReference<StringValue>(string);
    }

    public int hashCode()
    {
      return _hash;
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof UnserializeKey))
        return false;

      UnserializeKey key = (UnserializeKey) o;

      StringValue a = _stringRef.get();
      StringValue b = key._stringRef.get();

      if (a == null || b == null)
        return false;

      return a.equals(b);
    }
  }
}

