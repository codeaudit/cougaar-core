package org.cougaar.core.nameservice;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import javax.naming.spi.InitialContextFactory;
import org.cougaar.core.component.*;

public class NameServiceContextFactory implements InitialContextFactory {
    public Context getInitialContext(Hashtable env) {
        return null;
    }
}
