/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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
package org.cougaar.core.examples.servlet;

import java.io.*;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.component.*;
import org.cougaar.core.service.ServletService;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component registers a "/file" servlet that listens
 * for "/$agent/file?name=NAME" HTTP requests and sends
 * back the file contents (using the ConfigFinder).
 * <p>
 * Load into an agent's ".ini" with:<pre>
 *   plugin = org.cougaar.core.examples.servlet.FileServletComponent
 * </pre>
 * <p>
 * Example usage, if loaded into agent "3ID", to load the file named
 * "3ID.ini":<pre>
 *   http://localhost:8800/$3ID/file?name=3ID.ini
 * </pre>
 */
public class FileServletComponent
  extends GenericStateModelAdapter
  implements Component
{

  private ConfigFinder configFinder;
  private ServletService servletService;

  public void setParameter(Object o) {
    // no parameters
  }

  public void setBindingSite(BindingSite bs) {
  }

  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  public void load() {
    super.load();

    // should use a config-finder service; for now this is fine:
    configFinder = ConfigFinder.getInstance();

    try {
      servletService.register("/file", (new MyServlet()));
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to register servlet path \"/file\": "+e);
    }
  }

  private class MyServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {

      // get "?name=.." parameter
      String fileName = null;
      for (Enumeration en = request.getParameterNames();
           en.hasMoreElements();
          ) {
        String name = (String) en.nextElement();
        String values[] = request.getParameterValues(name);
        int nvalues = ((values != null) ? values.length : 0);
        if (nvalues > 0) {
          String value = values[nvalues - 1];
          if (name.equals("name")) {
            fileName = value;
          }
        }
      }

      if (fileName == null) {
        response.sendError(
          HttpServletResponse.SC_BAD_REQUEST, 
          "Must specify a \"?name=FILENAME\"");
        return;
      }

      InputStream fin;
      try {
        fin = configFinder.open(fileName);
      } catch (IOException ioe) {
        response.sendError(
          HttpServletResponse.SC_NOT_FOUND, 
          "Unable to open file \""+fileName+"\"");
        return;
      }

      String contentType = guessContentType(fileName, fin);
      if (contentType != null) {
        response.setContentType(contentType);
      }

      // maybe add client "last-modified" header?

      OutputStream out = response.getOutputStream();
      byte[] buf = new byte[1024];
      while (true) {
        int len = fin.read(buf);
        if (len < 0) {
          break;
        }
        out.write(buf, 0, len);
      }

      fin.close();
      out.flush();
      out.close();
    }

    private String guessContentType(
        String fileName, InputStream fin) throws IOException {
      // examine the first couple bytes of the stream:
      return URLConnection.guessContentTypeFromStream(fin);
      // or instead examine the filename extention (.gif, etc)
    }
  }
}
