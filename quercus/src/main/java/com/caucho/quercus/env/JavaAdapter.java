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

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.program.JavaClassDef;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.UnimplementedVException;
import edu.cmu.cs.varex.V;
import edu.cmu.cs.varex.VWriteStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface for marshalled Java data structures.
 */
abstract public class JavaAdapter extends ArrayValue
  implements Serializable
{
  private static final Logger log
    = Logger.getLogger(JavaAdapter.class.getName());

  private WeakReference<Env> _envRef;
  private Object _object;

  private JavaClassDef _classDef;

  protected JavaAdapter(Object object, JavaClassDef def)
  {
    _object = object;
    _classDef = def;
  }

  public JavaClassDef getClassDef()
  {
    return _classDef;
  }

  public Env getEnv()
  {
    return Env.getCurrent();
  }

  public Value wrapJava(Object obj)
  {
    return getEnv().wrapJava(obj);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toObject()
  {
    return null;
  }

  /**
   * Converts to a Java object.
   */
  @Override
  public Object toJavaObject()
  {
    return _object;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    if (type.isAssignableFrom(_object.getClass())) {
      return _object;
    }
    else {
      env.warning(L.l("Can't assign {0} to {1}",
              _object.getClass().getName(), type.getName()));

      return null;
    }
  }

  //
  // Conversions
  //

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    Value obj = env.createObject();

    for (VEntry entry : entrySet()) {
      Value key = entry.getKey();

      if (key instanceof StringValue) {
        // XXX: intern?
        obj.putField(env, key.toString(), entry.getEnvVar().getOne());
      }
    }

    return obj;
  }

  /**
   * Converts to a java List object.
   */
  @Override
  public Collection toJavaCollection(Env env, Class type)
  {
    Collection coll = null;

    if (type.isAssignableFrom(HashSet.class)) {
      coll = new HashSet();
    }
    else if (type.isAssignableFrom(TreeSet.class)) {
      coll = new TreeSet();
    }
    else {
      try {
        coll = (Collection) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }

   for (Map.Entry entry : objectEntrySet()) {
      coll.add(entry.getValue());
    }

    return coll;
  }

  /**
   * Converts to a java List object.
   */
  @Override
  public List toJavaList(Env env, Class type)
  {
    List list = null;

    if (type.isAssignableFrom(ArrayList.class)) {
      list = new ArrayList();
    }
    else if (type.isAssignableFrom(LinkedList.class)) {
      list = new LinkedList();
    }
    else if (type.isAssignableFrom(Vector.class)) {
      list = new Vector();
    }
    else {
      try {
        list = (List) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }

   for (Map.Entry entry : objectEntrySet()) {
      list.add(entry.getValue());
    }

    return list;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    Map map = null;

    if (type.isAssignableFrom(TreeMap.class)) {
      map = new TreeMap();
    }
    else if (type.isAssignableFrom(LinkedHashMap.class)) {
      map = new LinkedHashMap();
    }
    else {
      try {
        map = (Map) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);

        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }

    for (Map.Entry entry : objectEntrySet()) {
      map.put(entry.getKey(), entry.getValue());
    }

    return map;
  }

  /**
   * Copy for assignment.
   */
  @Override
  abstract public Value copy();

  /**
   * Copy for serialization
   */
  @Override
  abstract public Value copy(Env env, IdentityHashMap<Value, EnvVar> map);

  /**
   * Returns the size.
   */
  @Override
  abstract public V<? extends Integer> getSize();

  /**
   * Clears the array
   */
  @Override
  abstract public void clear();

  /**
   * Adds a new value.
   */
  @Override
  public final V<? extends ValueOrVar> put(FeatureExpr ctx, V<? extends ValueOrVar> value)
  {
    throw new UnimplementedVException();
//    return put(ctx, value);
  }

  /**
   * Adds a new value.
   */
  @Override
  public final V<? extends ValueOrVar> put(FeatureExpr ctx, Value key, V<? extends ValueOrVar> value)
  {
    return V.one(putImpl(key, value.getOne().toValue()));
  }

  /**
   * Adds a new value.
   */
  abstract public Value putImpl(Value key, Value value);

  /**
   * Add to front.
   */
  @Override
  public ArrayValue unshift(Value value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Splices.
   */
  @Override
  public ArrayValue splice(int begin, int end, ArrayValue replace)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public EnvVar getArg(Value index, boolean isTop)
  {
    return get(index);
  }

  /**
   * Sets the array ref.
   * @param ctx
   */
  @Override
  public V<? extends Var> putVar(FeatureExpr ctx)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creatse a tail index.
   * @param ctx
   */
  @Override
  abstract public V<? extends Value> createTailKey(FeatureExpr ctx);

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a new value.
   */
  @Override
  abstract public EnvVar get(Value key);

  /**
   * Removes a value.
   */
  @Override
  abstract public V<? extends Value> remove(FeatureExpr ctx, Value key);

  /**
   * Returns the array ref.
   */
  @Override
  public EnvVar getVar(FeatureExpr ctx, Value index)
  {
    throw new UnimplementedVException();
//    // php/0ceg - Since Java does not support references, the adapter
//    // just creates a new Var, but modifying the var will not modify
//    // the field
//
//    Var var = new VarImpl(V.one(new JavaAdapterVar(this, index)));
//
//    return new EnvVarImpl(V.one(var));
  }

  /**
   * Returns an iterator of the entries.
   */
  @Override
  public Set<Value> keySet()
  {
    return new KeySet(getEnv());
  }

  /**
   * Returns a set of all the entries.
   */
  @Override
  abstract public Set<VEntry> entrySet();

  /**
   * Returns a java object set of all the entries.
   */
  abstract public Set<Map.Entry<Object,Object>> objectEntrySet();

  /**
   * Returns a collection of the values.
   */
  @Override
  public Collection<EnvVar> values()
  {
    throw new UnimplementedException();
  }

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  @Override
  public ArrayValue append(Value key, EnvVar value)
  {
    put(key, value);

    return this;
  }


  /**
   * Pops the top value.
   */
  @Override
  public V<? extends Value> pop(Env env, FeatureExpr ctx)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Shuffles the array
   */
  @Override
  public Value shuffle()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the head.
   */
  @Override
  public Entry getHead()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the tail.
   */
  @Override
  protected Entry getTail()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the current value.
   */
  @Override
  public V<? extends Value> current()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the current key
   */
  @Override
  public V<? extends Value> key()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if there are more elements.
   */
  @Override
  public V<? extends Boolean> hasCurrent()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the next value.
   * @param ctx
   */
  @Override
  public V<? extends Value> next(FeatureExpr ctx)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the previous value.
   * @param ctx
   */
  @Override
  public V<? extends Value> prev(FeatureExpr ctx)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * The each iterator
   */
  @Override
  public Value each()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the first value.
   * @param ctx
   */
  @Override
  public V<? extends Value> reset(FeatureExpr ctx)
  {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the last value.
   * @param ctx
   */
  @Override
  public V<? extends Value> end(FeatureExpr ctx)
  {
    return V.one(BooleanValue.FALSE);
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  @Override
  public V<? extends Value> contains(Value value)
  {
                     throw new UnimplementedVException();
//    for (VEntry entry : entrySet()) {
//      if (entry.getEnvVar().equals(value))
//        return entry.getKey();
//    }
//
//    return NullValue.NULL;
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   */
  @Override
  public V<? extends Value> containsStrict(Value value)
  {
    throw new UnimplementedVException();
//    for (VEntry entry : entrySet()) {
//      if (entry.getEnvVar().getOne().eql(value))
//        return entry.getKey();
//    }
//
//    return NullValue.NULL;
  }

  /**
   * Returns the corresponding valeu if this array contains the given key
   *
   * @param key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   */
  @Override
  public V<? extends Value> containsKey(Value key)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an object array of this array.  This is a copy of this object's
   * backing structure.  Null elements are not included.
   *
   * @return an object array of this array
   */
//  @Override
//  public VEntry[] toEntryArray()
//  {
//    throw new UnsupportedOperationException();
//  }

  /**
   * Sorts this array based using the passed Comparator
   *
   * @param comparator the comparator for sorting the array
   * @param resetKeys  true if the keys should not be preserved
   * @param strict  true if alphabetic keys should not be preserved
   */
  @Override
  public void sort(Comparator<VEntry> comparator,
                   boolean resetKeys, boolean strict)
  {
    throw new UnimplementedVException();
//    VEntry[] entries = new Map.Entry[getSize()];
//
//    int i = 0;
//    for (VEntry entry : entrySet()) {
//      entries[i++] = entry;
//    }
//
//    Arrays.sort(entries, comparator);
//
//    clear();
//
//    long base = 0;
//
//    if (! resetKeys)
//      strict = false;
//
//    for (int j = 0; j < entries.length; j++) {
//      Value key = entries[j].getKey();
//
//      if (resetKeys && (! (key instanceof StringValue) || strict))
//        put(LongValue.create(base++), entries[j].getValue());
//      else
//        put(entries[j].getKey(), entries[j].getValue());
//    }
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Exports the value.
   */
  @Override
  protected void varExportImpl(StringValue sb, int level)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Resets all numerical keys with the first index as base
   *
   * @param base  the initial index
   * @param strict  if true, string keys are also reset
   */
  @Override
  public boolean keyReset(long base, boolean strict)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Takes the values of this array and puts them in a java array
   */
  @Override
  public Value[] valuesToArray()
  {
    Value[] values = new Value[getSize().getOne()];

    int i = 0;

    for (VEntry entry : entrySet()) {
      values[i++] = entry.getEnvVar().getOne();
    }

    return values;
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, FeatureExpr ctx, Class elementType)
  {
    int size = getSize().getOne();

    Object array = Array.newInstance(elementType, size);

    MarshalFactory factory = env.getModuleContext().getMarshalFactory();
    Marshal elementMarshal = factory.create(elementType);

    int i = 0;

    for (VEntry entry : entrySet()) {
      Array.set(array, i++, elementMarshal.marshal(env,
              ctx, entry.getEnvVar().getOne(),
                                                   elementType));
    }

    return array;
  }

  @Override
  public V<? extends Value> getField(Env env, StringValue name)
  {
    return V.one(_classDef.getField(env, this, name));
  }

  @Override
  public V<? extends Value> putField(Env env,
                                     FeatureExpr ctx, StringValue name,
                                     V<? extends ValueOrVar> value)
  {
    return V.one(_classDef.putField(env, ctx, this, name, value.getOne().toValue()));
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _classDef.getName();
  }

  @Override
  public boolean isA(Env env, String name)
  {
    return _classDef.isA(env, name);
  }

  /**
   * Returns the method.
   */
  @Override
  public AbstractFunction findFunction(StringValue methodName)
  {
    return _classDef.findFunction(methodName);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public V<? extends ValueOrVar> callMethod(Env env, FeatureExpr ctx, StringValue methodName, int hash,
                                            V<? extends ValueOrVar>[] args)
  {
    return _classDef.callMethod(env, ctx, this,
                                methodName, hash,
                                args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public @Nonnull
  V<? extends ValueOrVar> callMethod(Env env, FeatureExpr ctx, StringValue methodName, int hash)
  {
    return _classDef.callMethod(env, ctx, this, methodName, hash);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public @Nonnull V<? extends ValueOrVar> callMethodRef(Env env,
                                FeatureExpr ctx, StringValue methodName, int hash,
                                                   V<? extends ValueOrVar>[] args)
  {
    return _classDef.callMethod(env, ctx, this, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public @Nonnull
  V<? extends ValueOrVar> callMethodRef(Env env, FeatureExpr ctx, StringValue methodName, int hash)
  {
    return _classDef.callMethod(env, ctx, this, methodName, hash);
  }


  @Override
  public void varDumpImpl(Env env, FeatureExpr ctx,
                          VWriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet) {
    out.println(ctx, "array(" + getSize() + ") {");

    int nestedDepth = depth + 1;

    for (VEntry mapEntry : entrySet()) {
      FeatureExpr innerCtx = ctx.and(mapEntry.getCondition());
      printDepth(innerCtx, out, nestedDepth * 2);
      out.print(innerCtx, "[");

      Value key = mapEntry.getKey();

      if (key.isString())
        out.print(innerCtx, "\"" + key + "\"");
      else
        out.print(innerCtx, key);

      out.println(innerCtx, "]=>");

      printDepth(innerCtx, out, nestedDepth * 2);

      mapEntry.getEnvVar().getValue().sforeach(innerCtx, (c, a) -> a.varDump(env, c, out, nestedDepth, valueSet));

      out.println(innerCtx);
    }

    printDepth(ctx, out, 2 * depth);

    out.print(ctx, "}");
  }

  @Override
  protected void printRImpl(Env env, FeatureExpr ctx,
                            VWriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet) {
    out.println(ctx, "Array");
    printDepth(ctx, out, 8 * depth);
    out.println(ctx, "(");

    for (VEntry mapEntry : entrySet()) {
      FeatureExpr innerCtx = ctx.and(mapEntry.getCondition());
      printDepth(innerCtx, out, 8 * depth);

      out.print(innerCtx, "    [");
      out.print(innerCtx, mapEntry.getKey());
      out.print(innerCtx, "] => ");

      V<? extends Value> value = mapEntry.getEnvVar().getValue();

      value.sforeach(innerCtx, (c, v) -> {
        if (v != null)
          v.printR(env, c, out, depth + 1, valueSet);
      });
      out.println(ctx);
    }

    printDepth(ctx, out, 8 * depth);
    out.println(ctx, ")");
  }

  //
  // Java Serialization
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(_object);
    out.writeObject(_classDef.getName());
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _envRef = new WeakReference<Env>(Env.getInstance());

    _object = in.readObject();
    _classDef = getEnv().getJavaClassDefinition((String) in.readObject());
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return String.valueOf(_object);
  }

  public class KeySet extends AbstractSet<Value> {
    Env _env;

    KeySet(Env env)
    {
      _env = env;
    }

    @Override
    public int size()
    {
      return getSize().getOne();
    }

    @Override
    public Iterator<Value> iterator()
    {
      return getKeyIterator(_env);
    }
  }
}

