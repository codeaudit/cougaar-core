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

package org.cougaar.core.naming;

import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context; // inlined
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.NamingService;

public class NamingServiceProvider implements ServiceProvider {
    private Hashtable env;
    private class Impl implements NamingService {
        public InitialDirContext getRootContext() throws NamingException {
            return new InitialDirContext(env);
        }
    }

    public NamingServiceProvider(Hashtable env) throws NamingException {
        if (env.get(Context.INITIAL_CONTEXT_FACTORY) == null) {
            this.env = new Hashtable(env);
            this.env.put(Context.INITIAL_CONTEXT_FACTORY,
                         NamingServiceFactory.class.getName());
        } else {
            this.env = env;
        }
    }

    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass != NamingService.class)
            throw new IllegalArgumentException(getClass() + " does not furnish "
                                               + serviceClass);
        return new Impl();
    }

    public void releaseService(ServiceBroker sb, Object requestor,
                               Class serviceClass, Object service)
    {
    }
}
