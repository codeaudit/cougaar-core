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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.util.ConfigFinder;

/**
 * A servlet that reads a reads a file from the local ConfigFinder
 * and sends the contents back to the client.
 * <p>
 * Load into an agent's ".xml" with:<pre>
 *   &lt;component 
 *     class="org.cougaar.core.examples.servlet.FileServerServlet"&gt;
 *     &lt;argument&gt;/file&lt;/argument&gt; 
 *   &lt;/component&gt;
 * </pre>
 * <p>
 * For example, if this servlet is loaded into agent "X", and the
 * user wanted to read a file named "SimpleAgent.xsl":<pre>
 *   http://localhost:8800/$X/file?name=SimpleAgent.xsl
 * </pre>
 * <p>
 * Note that this example ignores security issues.
 */
public class FileServerServlet extends ComponentServlet {

  @Override
public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    // get "?name=.." parameter
    String fileName = request.getParameter("name");
    if (fileName != null) {
      fileName = fileName.trim();
      if (fileName.length() == 0) {
        fileName = null;
      }
    }
    if (fileName == null) {
      response.sendError(
          HttpServletResponse.SC_BAD_REQUEST, 
          "Must specify a \"?name=FILENAME\"");
      return;
    }

    // should use a config-finder service; for now this is fine:
    ConfigFinder configFinder = ConfigFinder.getInstance();

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
