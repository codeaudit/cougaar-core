/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.service.wp;

import java.io.Serializable;
import java.net.URI;

/**
 * An AddressEntry represents a single entry in the white pages.
 * <p>
 * Address entries are immutable.
 */
public final class AddressEntry implements Serializable {

  /**
   * A zero-length array of address entries, for use
   * by the white pages and clients.
   */
  public static final AddressEntry[] EMPTY_ARRAY = 
    new AddressEntry[0];

  private final String name;
  private final Application app;
  private final URI uri;
  private final Cert cert;
  private final long ttl;
  private transient int _hc;

  public AddressEntry(
      String name, Application app, URI uri, Cert cert, long ttl) {
    this.name = name;
    this.app = app;
    this.uri = uri;
    this.cert = cert;
    this.ttl = ttl;
    if (name==null || app==null || uri==null || cert==null) {
      throw new IllegalArgumentException(
          "Null argument");
    }
  }

  /** @return the non-null name (e.g.: "p.X.A") */
  public String getName() { return name; }

  /** @return the non-null application (e.g.: "WP") */
  public Application getApplication() { return app; }

  /** @return the non-null address (e.g.: "rmi://foo.com:123/xyz") */
  public URI getAddress() { return uri; }

  /** @return the non-null cert (e.g.: Cert.NULL) */
  public Cert getCert() { return cert; }

  /**
   * @return the lease "time to live" in milliseconds
   * (e.g.: midnight jan 1st 2003)
   */
  public long getTTL() { return ttl; }

  public String toString() {
    return 
      "(name="+name+
      " app="+app+
      " uri="+uri+
      " cert="+cert+
      " ttl="+ttl+")";
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AddressEntry)) {
      return false;
    } else {
      AddressEntry ae = (AddressEntry) o;
      return 
        name.equals(ae.name) &&
        app.equals(ae.app) &&
        cert.equals(ae.cert) &&
        uri.equals(ae.uri) &&
        (ttl==ae.ttl);
    }
  }

  public int hashCode() {
    if (_hc == 0) {
      int h = 0;
      h = 31*h + name.hashCode();
      h = 31*h + app.hashCode();
      h = 31*h + uri.hashCode();
      h = 31*h + cert.hashCode();
      h = 31*h + (int)ttl;
      _hc = h;
    }
    return  _hc;
  }
}
