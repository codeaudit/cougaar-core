package org.cougaar.core.nameservice;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import org.cougaar.core.component.*;

public class NameServiceProvider implements ServiceProvider {
    private Hashtable env;
    private class Impl implements NameService {
        public InitialDirContext getRootContext() throws NamingException {
            return new InitialDirContext(env);
        }
    }

    public NameServiceProvider(Hashtable env) throws NamingException {
        if (env.get(Context.INITIAL_CONTEXT_FACTORY) == null) {
            this.env = new Hashtable(env);
            this.env.put(Context.INITIAL_CONTEXT_FACTORY,
                         NameServiceContextFactory.class.getName());
        } else {
            this.env = env;
        }
    }

    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass != NameService.class)
            throw new IllegalArgumentException(getClass() + " does not furnish "
                                               + serviceClass);
        return new Impl();
    }

    public void releaseService(ServiceBroker sb, Object requestor,
                               Class serviceClass, Object service)
    {
    }
}
