/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.naming;


import org.cougaar.core.naming.*;
import org.cougaar.core.node.Communications;

import org.cougaar.util.log.*;
import org.cougaar.bootstrap.SystemProperties;

import java.util.*;
import javax.naming.directory.*;
import javax.naming.spi.*;
import javax.naming.*;

/**
 * Naming context factory that wraps the SUN LDAP factory for Cougaar.  Properties can be set in
 * alpreg.ini or by java VM properties.
 * @property org.cougaar.nameserver.baseDN The distinguished name to use as the "root" for this factory.  Defaults
 *           to </em>dc=cougaar,dc=org</em>
 * @property java.naming.provider.url The URL of the LDAP directory.  Defaults
 *           to </em>ldap://localhost:389</em>
 * @property java.naming.security.authentication The type of authentication to use with the LDAP directory.  Defaults
 *           to </em>simple</em>
 * @property java.naming.security.principal User name to use to connect to the LDAP directory.  Defaults
 *           to </em>cn=manager, dc=cougaar, dc=org</em>
 * @property java.naming.security.credentials The password to use to connect to the LDAP directory.  Defaults
 *           to </em>secret</em>
 */

public class LdapCtxFactory implements InitialContextFactory {

    private InitialContextFactory nsf;
    private Communications alpreg_ini;
    
    private String providerURL;
    private String securityAuthentication;
    private String securityPrincipal;
    private String securityCredentials;
    private String baseDN;
    private static Logger log;
    
    public LdapCtxFactory() {
        if (log == null)
            log = Logging.getLogger(this.getClass().getName());
        
        if (log.isDebugEnabled()) log.debug("Creating factory...");
        
        // Avoid compile-time dependency on ldap.jar
        try {
            Class c = Class.forName("com.sun.jndi.ldap.LdapCtxFactory");
            nsf = (com.sun.jndi.ldap.LdapCtxFactory)c.newInstance();
        } catch (Exception ex) {
            if (log.isFatalEnabled()) log.fatal("Error initializing Sun LdapCtxFactory", ex);
            throw new RuntimeException(ex);
        }
        
        nsf = new com.sun.jndi.ldap.LdapCtxFactory();
        alpreg_ini = Communications.getInstance();

        Properties envProps = SystemProperties.getSystemPropertiesWithPrefix("java.naming");
        
        // set defaults
        if (alpreg_ini.get(Context.PROVIDER_URL) == null) 
            alpreg_ini.put(Context.PROVIDER_URL, envProps.getProperty(Context.PROVIDER_URL, "ldap://localhost:389"));
        if (alpreg_ini.get(Context.SECURITY_AUTHENTICATION) == null) 
            alpreg_ini.put(Context.SECURITY_AUTHENTICATION, envProps.getProperty(Context.SECURITY_AUTHENTICATION, "simple"));
        if (alpreg_ini.get(Context.SECURITY_PRINCIPAL) == null) 
            alpreg_ini.put(Context.SECURITY_PRINCIPAL, envProps.getProperty(Context.SECURITY_PRINCIPAL, "cn=manager, dc=cougaar, dc=org"));
        if (alpreg_ini.get(Context.SECURITY_CREDENTIALS) == null) 
            alpreg_ini.put(Context.SECURITY_CREDENTIALS, envProps.getProperty(Context.SECURITY_CREDENTIALS,"secret"));
        if (alpreg_ini.get("org.cougaar.nameserver.baseDN") == null) 
            alpreg_ini.put("org.cougaar.nameserver.baseDN", System.getProperty("org.cougaar.nameserver.baseDN", "dc=cougaar,dc=org"));
        
        providerURL=            alpreg_ini.get(Context.PROVIDER_URL);
        securityAuthentication= alpreg_ini.get(Context.SECURITY_AUTHENTICATION);
        securityPrincipal=      alpreg_ini.get(Context.SECURITY_PRINCIPAL);
        securityCredentials=    alpreg_ini.get(Context.SECURITY_CREDENTIALS);
        baseDN=                 alpreg_ini.get("org.cougaar.nameserver.baseDN");

        if (log.isDebugEnabled()) log.debug("providerURL="+providerURL+
                                            "; securityAuthentication="+securityAuthentication+
                                            "; securityPrincipal="+securityPrincipal+
                                            "; securityCredentials="+securityCredentials+
                                            "; baseDN="+baseDN);
        

    }

    public javax.naming.Context getInitialContext(java.util.Hashtable hashtable) throws javax.naming.NamingException {
        if (log.isDebugEnabled()) log.debug("Getting context...");
        
        hashtable.put(Context.PROVIDER_URL, providerURL);

        /* specify authentication information */
        hashtable.put(Context.SECURITY_AUTHENTICATION, securityAuthentication);
        hashtable.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        hashtable.put(Context.SECURITY_CREDENTIALS, securityCredentials);
    
        DirContext realDirContext = (DirContext)nsf.getInitialContext(hashtable);
        
        realDirContext = (DirContext)realDirContext.lookup(baseDN);
        

        return new LdapDirContext(realDirContext);
    }
    
}
