/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Cert;
import org.cougaar.core.util.UID;
import sun.misc.BASE64Encoder;

/**
 * Encode Bundles and Certs to Strings.
 *
 * @see BundleDecoder
 */
public final class BundleEncoder {

  private BundleEncoder() {}

  public static String encodeBundle(Bundle b) {
    StringBuffer buf = new StringBuffer();
    buf.append("name=").append(b.getName());
    buf.append(" uid=").append(b.getUID());
    buf.append(" ttd=").append(b.getTTD());
    buf.append(" entries=");
    Map entries = b.getEntries();
    if (entries == null) {
      buf.append("null");
    } else {
      buf.append("{");
      int n = entries.size();
      if (n > 0) {
        Iterator iter = entries.entrySet().iterator();
        for (int i = 0; i < n; i++) {
          Map.Entry me = (Map.Entry) iter.next();
          String type = (String) me.getKey();
          AddressEntry ae = (AddressEntry) me.getValue();
          buf.append(type).append("=");
          Cert cert = ae.getCert();
          boolean null_cert = Cert.NULL.equals(cert);
          if (!null_cert) {
            buf.append("(uri=");
          }
          buf.append(ae.getURI());
          if (!null_cert) {
            buf.append(" cert=");
            buf.append(encodeCert(cert));
            buf.append(")");
          }
          if ((i+1) < n) {
            buf.append(", ");
          }
        }
      }
      buf.append("}");
    }
    return buf.toString();
  }

  public static String encodeCert(Cert cert) {
    if (cert == null || cert.equals(Cert.NULL)) {
      return "NULL";
    }
    if (cert.equals(Cert.PROXY)) {
      return "PROXY";
    }
    try {
      if (cert instanceof Cert.Indirect) {
        String q = ((Cert.Indirect) cert).getQuery();
        String v = URLEncoder.encode(q, "UTF-8");
        return "Indirect:"+v;
      }
      if (cert instanceof Cert.Direct) {
        Certificate c = ((Cert.Direct) cert).getCertificate();
        byte[] ba = c.getEncoded();
        String v = (new BASE64Encoder()).encode(ba);
        return "Direct:"+v;
      }
      // a custom cert type -- serialize it!
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(cert);
      oos.flush();
      byte[] ba = baos.toByteArray();
      String v = (new BASE64Encoder()).encode(ba);
      return "Object:"+v;
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to encodeCert("+cert+")", e);
    }
  }
}
