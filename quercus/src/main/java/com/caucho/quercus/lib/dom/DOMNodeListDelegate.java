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

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.*;
import de.fosd.typechef.featureexpr.FeatureExpr;
import edu.cmu.cs.varex.VHelper;

import java.util.Iterator;

public class DOMNodeListDelegate
  implements TraversableDelegate
{
  public DOMNodeListDelegate()
  {
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListKeyIterator((DOMNodeList) obj.toJavaObject());
  }
  
  @Override
  public Iterator<EnvVar> getValueIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListValueIterator(env,  (DOMNodeList) obj.toJavaObject());
  }
  
  @Override
  public Iterator<VEntry> getIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListIterator(env, (DOMNodeList) obj.toJavaObject());
  }
  
  public class DOMNodeListKeyIterator
    implements Iterator<Value>
  {
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListKeyIterator(DOMNodeList list)
    {
      _list = list;
    }
    
    @Override
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    @Override
    public Value next()
    {
      return LongValue.create(_index++);
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListValueIterator
    implements Iterator<EnvVar>
  {
    private Env _env;
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListValueIterator(Env env, DOMNodeList list)
    {
      _env = env;
      _list = list;
    }
    
    @Override
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    @Override
    public EnvVar next()
    {
      return EnvVar._gen( _env.wrapJava(_list.item(_index++)));
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListIterator
    implements Iterator<VEntry>
  {
    private Env _env;
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListIterator(Env env, DOMNodeList list)
    {
      _env = env;
      _list = list;
    }
    
    @Override
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    @Override
    public VEntry next()
    {
      return new DOMNodeListEntry(_index, EnvVar._gen(_env.wrapJava(_list.item(_index++))));
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListEntry
    implements VEntry
  {
    private int _key;
    private EnvVar _value;
    
    public DOMNodeListEntry(int index, EnvVar value)
    {
      _key = index;
      _value = value;
    }
    
    @Override
    public Value getKey()
    {
      return LongValue.create(_key);
    }

    @Override
    public FeatureExpr getCondition() {
      return VHelper.noCtx();
    }

    @Override
    public EnvVar getEnvVar()
    {
      return _value;
    }
    
    @Override
    public EnvVar setEnvVar(EnvVar value)
    {
      throw new UnsupportedOperationException();
    }
  }
}
