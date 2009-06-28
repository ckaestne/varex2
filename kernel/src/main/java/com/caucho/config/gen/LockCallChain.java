/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
package com.caucho.config.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.Synchronization;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents EJB lock type specification interception. The specification gears
 * it towards EJB singletons, but it can be used for other bean types.
 */
public class LockCallChain extends AbstractCallChain {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(LockCallChain.class);

  private BusinessMethodGenerator businessMethod;
  private EjbCallChain next;

  private TransactionAttributeType transactionAttribute;
  private boolean isContainerManaged = true;
  private boolean isSynchronization;

  public LockCallChain(BusinessMethodGenerator businessMethod, EjbCallChain next) {
    super(next);

    this.businessMethod = businessMethod;
    this.next = next;

    isContainerManaged = businessMethod.isXaContainerManaged();
  }

  protected BusinessMethodGenerator getBusinessMethod() {
    return businessMethod;
  }

  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced() {
    return (isContainerManaged && transactionAttribute != null && !transactionAttribute
        .equals(TransactionAttributeType.SUPPORTS));
  }

  /**
   * Returns the transaction type
   */
  public TransactionAttributeType getTransactionType() {
    return transactionAttribute;
  }

  /**
   * Introspects the method for the default values
   */
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod) {
    ApiClass apiClass = apiMethod.getDeclaringClass();

    TransactionManagement xaManagement = apiClass
        .getAnnotation(TransactionManagement.class);

    if (xaManagement != null
        && xaManagement.value() != TransactionManagementType.CONTAINER) {
      isContainerManaged = false;
      return;
    }

    ApiClass implClass = null;

    if (implMethod != null)
      implClass = implMethod.getDeclaringClass();

    if (implClass != null
        && Synchronization.class.isAssignableFrom(implClass.getJavaClass())) {
      isSynchronization = true;
    }

    TransactionAttribute xaAttr;

    xaAttr = apiMethod.getAnnotation(TransactionAttribute.class);

    if (xaAttr == null) {
      xaAttr = apiClass.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr == null && implMethod != null) {
      xaAttr = implMethod.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr == null && implClass != null) {
      xaAttr = implClass.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr != null)
      transactionAttribute = xaAttr.value();
  }

  /**
   * Generates the static class prologue
   */
  @SuppressWarnings("unchecked")
  @Override
  public void generatePrologue(JavaWriter out, HashMap map) throws IOException {
    if (isContainerManaged && map.get("caucho.ejb.xa") == null) {
      map.put("caucho.ejb.xa", "done");

      out.println();
      out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
      out.println("  = new com.caucho.ejb3.xa.XAManager();");
    }

    next.generatePrologue(out, map);
  }

  /**
   * Generates the method interceptor code
   */
  @SuppressWarnings("unchecked")
  public void generateCall(JavaWriter out) throws IOException {
    boolean isPushDepth = false;

    if (isContainerManaged && transactionAttribute != null) {
      switch (transactionAttribute) {
      case MANDATORY: {
        out.println("_xa.beginMandatory();");
      }
        break;

      case NEVER: {
        out.println("_xa.beginNever();");
      }
        break;

      case NOT_SUPPORTED: {
        out.println("Transaction xa = _xa.beginNotSupported();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;

      case REQUIRED: {
        out.println("Transaction xa = _xa.beginRequired();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;

      case REQUIRES_NEW: {
        out.println("Transaction xa = _xa.beginRequiresNew();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;
      }
    }

    if (isSynchronization) {
      out.println("_xa.registerSynchronization(_bean);");
    }

    generateNext(out);

    if (isContainerManaged && transactionAttribute != null) {
      if (isPushDepth)
        out.popDepth();

      for (Class exn : businessMethod.getApiMethod().getExceptionTypes()) {
        ApplicationException appExn = (ApplicationException) exn
            .getAnnotation(ApplicationException.class);

        if (appExn == null)
          continue;

        if (!RuntimeException.class.isAssignableFrom(exn) && appExn.rollback()) {
          out.println("} catch (" + exn.getName() + " e) {");
          out.println("  _xa.markRollback(e);");
          out.println("  throw e;");
        } else if (RuntimeException.class.isAssignableFrom(exn)
            && !appExn.rollback()) {
          out.println("} catch (" + exn.getName() + " e) {");
          out.println("  throw e;");
        }
      }

      switch (transactionAttribute) {
      case REQUIRED:
      case REQUIRES_NEW: {
        out.println("} catch (RuntimeException e) {");
        out.println("  _xa.markRollback(e);");
        out.println("  throw e;");
      }
      }

      switch (transactionAttribute) {
      case NOT_SUPPORTED: {
        out.println("} finally {");
        out.println("  if (xa != null)");
        out.println("    _xa.resume(xa);");
        out.println("}");
      }
        break;

      case REQUIRED: {
        out.println("} finally {");
        out.println("  if (xa == null)");
        out.println("    _xa.commit();");
        out.println("}");
      }
        break;

      case REQUIRES_NEW: {
        out.println("} finally {");
        out.println("  _xa.endRequiresNew(xa);");
        out.println("}");
      }
        break;
      }
    }
  }

  protected void generateNext(JavaWriter out) throws IOException {
    next.generateCall(out);
  }
}