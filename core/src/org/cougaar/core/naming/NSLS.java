package org.cougaar.core.naming;

import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;
import org.cougaar.core.component.*;
import org.cougaar.core.society.rmi.RMINameServer;

public class NSLS {
    public static void main(String[] args) {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                RMINameServer.class.getName());
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
            if (object instanceof DirContext) {
                listBindings((DirContext) object, indent + "  ");
            }
        }
    }
}
