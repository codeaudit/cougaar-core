/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
import javax.naming.*;
import javax.naming.directory.*;
import org.cougaar.core.component.*;

public class NSLS {
    public static void main(String[] args) {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                NamingServiceFactory.class.getName());
        try {
            DirContext ctx = new InitialDirContext(env);
            listBindings(ctx, "");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }

    private static void list(DirContext ctx, String indent) throws NamingException {
        NamingEnumeration enum = ctx.list("");
        while (enum.hasMoreElements()) {
            NameClassPair ncp = (NameClassPair) enum.nextElement();
            System.out.println(indent + ncp.getName() + ": " + ncp.getClassName());
            Attributes attributes = ctx.getAttributes(ncp.getName());
            if (attributes != null) {
                for (NamingEnumeration attrs = attributes.getAll(); attrs.hasMore(); ) {
                    Attribute attr = (Attribute) attrs.next();
                    System.out.println(indent + "    " + attr);
                }
            }
            try {
                Class cls = Class.forName(ncp.getClassName());
                if (DirContext.class.isAssignableFrom(cls)) {
                    list((DirContext) ctx.lookup(ncp.getName()), indent + "  ");
                }
            } catch (ClassNotFoundException cnfe) {
            }
        }
    }

    private static void listBindings(DirContext ctx, String indent)
        throws NamingException
    {
        NamingEnumeration enum = ctx.listBindings("");
        while (enum.hasMoreElements()) {
            Binding binding = (Binding) enum.nextElement();
            Object object = binding.getObject();
            System.out.println(indent + binding.getName() + ": " + object);
            Attributes attributes = ctx.getAttributes(binding.getName());
            if (attributes != null) {
                for (NamingEnumeration attrs = attributes.getAll(); attrs.hasMore(); ) {
                    Attribute attr = (Attribute) attrs.next();
                    System.out.println(indent + "    " + attr);
                }
            }
            if (object instanceof DirContext) {
                listBindings((DirContext) object, indent + "  ");
            }
        }
    }
}







