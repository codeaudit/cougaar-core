/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.domain;

import java.io.PrintStream;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactorySupport;

/**
 * A simple BinderFactory for binding domain Domains.
 **/
public class DefaultDomainBinderFactory extends BinderFactorySupport
{

  /** Select the binder to use - must be an extension of DefaultDomainBinder.
   **/
  protected Class getBinderClass(Object child) {
    return DefaultDomainBinder.class;
  }
  
  /** Bind a domain with a domain binder. 
   **/
  protected Binder bindChild(Class binderClass, Object child) {
    Binder b = super.bindChild(binderClass, child);
    if (b == null) {
      return null;
    } else {
      if (b instanceof DefaultDomainBinder) {
        return b;
      } else {
        System.err.println("Illegal binder class specified: "+binderClass);
        return null;
      }
    }
  }

}
