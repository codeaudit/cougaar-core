/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.service.wp;

import java.io.Serializable;
import java.security.cert.Certificate;

/**
 * A Cert holds the certification for an {@link AddressEntry}.
 * <p>
 * There are currently four built-in certs:<ul>
 *   <li>{@link #NULL}</li>
 *   <li>{@link #PROXY}</li>
 *   <li>{@link Cert.Direct}</li>
 *   <li>{@link Cert.Indirect}</li>
 * </ul>
 * User-defined Cert subclasses are also permitted, which
 * must be <u>immutable</u>.
 */
public abstract class Cert implements Serializable {

  /**
   * A "null" cert indicates that the client should contact the
   * entry address with no security (ie "in the open").
   */
  public static final Cert NULL = new Cert() {
    private Object readResolve() { return NULL; }
    public String toString() { return "null_cert"; }
  };

  /**
   * A "proxy" cert indicates that the client should contact
   * the agent with the name "CertProvider".
   * <p>
   * The cert provider agent will never specify a PROXY cert.
   */
  public static final Cert PROXY = new Cert() {
    private Object readResolve() { return PROXY; }
    public String toString() { return "proxy_cert"; }
  };

  /**
   * A "direct" cert contains a JAAS Crypto certificate.
   */
  public static final class Direct extends Cert {
    private final Certificate cert;
    private transient int _hc;
    public Direct(Certificate cert) { 
      this.cert = cert;
      if (cert == null) {
        throw new IllegalArgumentException("null cert");
      }
    }
    public Certificate getCertificate() { return cert; }
    public String toString() { return "(cert="+cert+")"; }
    public boolean equals(Object o) {
      return 
        (o == this || 
         (o instanceof Direct && cert.equals(((Direct)o).cert)));
    }
    public int hashCode() {
      if (_hc == 0) _hc = cert.hashCode();
      return _hc;
    }
  }

  /**
   * An "indirect" cert contains a query string which the client
   * should use to lookup the certificate.
   * <p>
   * The lookup location is application specific, but typically
   * refers to either a local file name, a distributed certificate
   * authority, or a yellow pages.
   */
  public static final class Indirect extends Cert {
    private final String query;
    public Indirect(String query) {
      this.query = query;
      if (query == null) {
        throw new IllegalArgumentException("null query");
      }
    }
    public String getQuery() { return query; }
    public String toString() { return "(query="+query+")"; }
    public boolean equals(Object o) {
      return
        (o == this ||
         (o instanceof Indirect && query.equals(((Indirect)o).query)));
    }
    public int hashCode() { return query.hashCode(); }
  }
}
