package org.cougaar.core.nameservice;

import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import org.cougaar.core.component.Service;

public interface NameService extends Service {
    InitialDirContext getRootContext() throws NamingException;
}
