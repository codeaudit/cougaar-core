package org.cougaar.core.naming;

import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import org.cougaar.core.component.Service;

public interface NamingService extends Service {
    InitialDirContext getRootContext() throws NamingException;
}
