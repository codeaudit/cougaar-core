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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Cert;
import org.cougaar.core.util.UID;
import sun.misc.BASE64Decoder;

/**
 * Decode Bundles and Certs from encoded Strings.
 *
 * @see BundleEncoder 
 */
public final class BundleDecoder {

  private static final String S1 =
    "^\\s*"+
    "name=(\\S+)"+
    "\\s+"+
    "("+
    "uid=(\\S+)"+
    "\\s+"+
    ")?"+
    "ttd=(\\d+)"+
    "\\s+"+
    "entries=(.*)"+
    "\\s*$";
  private static final String S2 =
    "^\\s*"+
    "([^\\s=]+)="+
    "(\\(uri=)?"+
    "(\\S+)"+
    "(\\s+cert=(\\S+)\\s*\\))?"+
    "\\s*"+
    "(,(.*)|$)";

  private static final Pattern P1 = Pattern.compile(S1);
  private static final Pattern P2 = Pattern.compile(S2);

  private static final String BEGIN_CERT =
    "-----BEGIN CERTIFICATE-----\n";
  private static final String END_CERT =
    "\n-----END CERTIFICATE-----";

  private BundleDecoder() {}

  public static Bundle decodeBundle(String s) {
    Matcher m1 = P1.matcher(s);
    if (!m1.matches()) {
      throw new RuntimeException(
          "String \""+s+"\" doesn't match pattern \""+S1+"\"");
    }
    String name = m1.group(1);
    String suid = m1.group(3);
    UID uid = 
      (suid == null || "null".equals(suid) ?
       (null) :
       UID.toUID(suid));
    String sttd = m1.group(4);
    long ttd = Long.parseLong(sttd);
    String sentries = m1.group(5);
    Map entries;
    if (sentries == null || "null".equals(sentries)) {
      entries = null;
    } else if (
        !sentries.startsWith("{") ||
        !sentries.endsWith("}")) {
      throw new RuntimeException(
          "Entries string \""+sentries+
          "\" doesn't startWith(\"{\") and endWith(\"}\"");
    } else {
      sentries = sentries.substring(1, sentries.length()-1);
      entries = new HashMap();
      int i = 0;
      while (true) {
        Matcher m2 = P2.matcher(sentries.substring(i));
        if (!m2.matches()) {
          throw new RuntimeException(
              "AddressEntry string["+i+"] \""+sentries.substring(i)+
              "\" doesn't match pattern \""+S2+"\"");
        }
        String type = m2.group(1);
        String suri = m2.group(3);
        URI uri = URI.create(suri);
        String scert = m2.group(5);
        Cert cert;
        if (scert == null) {
          cert = Cert.NULL;
        } else if (m2.group(2) != null) {
          cert = decodeCert(scert);
        } else {
          throw new RuntimeException(
              "Cert string \""+scert+"\" not in \"cert=\""+
              " of entries string \""+sentries+"\"");
        }
        AddressEntry ae = AddressEntry.getAddressEntry(
            name, type, uri, cert);
        entries.put(type, ae);
        String tail = m2.group(7);
        if (tail == null) {
          break;
        }
        i += 1 + m2.start(7);
      }
    }
    Bundle b = new Bundle(name, uid, ttd, entries);
    return b;
  }

  public static Cert decodeCert(String scert) {
    if (scert == null || scert.equalsIgnoreCase("NULL")) {
      return Cert.NULL;
    }
    if ("PROXY".equalsIgnoreCase(scert)) {
      return Cert.PROXY;
    }
    int i = scert.indexOf(":");
    if (i < 0) {
      throw new RuntimeException(
          "Encoded cert \""+scert+"\" lacks ':'");
    }
    String id = scert.substring(0, i);
    String v = scert.substring(i+1);
    try {
      if (id.equalsIgnoreCase("Indirect")) {
        String q = URLDecoder.decode(v, "UTF-8");
        return new Cert.Indirect(q);
      }
      if (id.equalsIgnoreCase("Direct")) {
        if (!v.startsWith(BEGIN_CERT)) {
          v = BEGIN_CERT + v;
        }
        if (!v.endsWith(END_CERT)) {
          v += END_CERT;
        }
        byte[] ba = v.getBytes(); // specify charsetName?
        ByteArrayInputStream is = new ByteArrayInputStream(ba);
        String cftype = "X.509"; // make a parameter?
        CertificateFactory cf = CertificateFactory.getInstance(cftype);
        Certificate c = cf.generateCertificate(is);
        return new Cert.Direct(c);
      }
      if (id.equalsIgnoreCase("Object")) {
        byte[] ba = (new BASE64Decoder()).decodeBuffer(v);
        ByteArrayInputStream is = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(is);
        return (Cert) ois.readObject();
      }
      throw new RuntimeException("Unknown type: "+id);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to decodeCert("+scert+")", e);
    }
  }
}
