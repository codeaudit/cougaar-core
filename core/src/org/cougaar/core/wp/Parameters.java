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

package org.cougaar.core.wp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to parse component parameters.
 * <p> 
 * Similar to org.cougaar.core.component.ParameterizedComponent,
 * will probably be moved into that package...
 */
public class Parameters {

  private final String prefix;
  private final Map m;

  public Parameters(Object o) {
    this(o, null);
  }

  public Parameters(Object o, String prefix) {
    this.prefix = prefix;
    m = parseMap(o);
  }

  protected Map parseMap(Object o) {
    if (!(o instanceof List)) {
      return Collections.EMPTY_MAP;
    }
    List l = (List) o;
    int n = l.size();
    if (n == 0) {
      return Collections.EMPTY_MAP;
    }
    Map ret = new HashMap(n);
    for (int i = 0; i < n; i++) {
      String s = (String) l.get(i);
      int sepIdx = s.indexOf('=');
      if (sepIdx < 0) {
        continue;
      }
      String key = s.substring(0, sepIdx);
      String value = s.substring(sepIdx+1);
      ret.put(key, value);
    }
    return ret;
  }

  public String getString(String key) {
    return getString(key, null);
  }

  public String getString(String key, String deflt) {
    String value = (String) m.get(key);
    if (value == null && prefix != null) {
      value = System.getProperty(prefix+key);
    }
    return (value == null ? deflt : value);
  }

  public boolean getBoolean(String key, boolean deflt) {
    String value = getString(key);
    return (value == null ? deflt : "true".equals(value));
  }

  public int getInt(String key, int deflt) {
    String value = getString(key);
    return (value == null ? deflt : Integer.parseInt(value));
  }

  public long getLong(String key, long deflt) {
    String value = getString(key);
    return (value == null ? deflt : Long.parseLong(value));
  }

  public double getDouble(String key, double deflt) {
    String value = getString(key);
    return (value == null ? deflt : Double.parseDouble(value));
  }

  public String toString() {
    return 
      "(parameters"+
      (prefix == null ? "" : (" prefix="+prefix))+
      " "+m+")";
  }
}
