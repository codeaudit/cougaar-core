/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
/*
 * Stand alone java client that opens up a URL connection, reads an object stream and 
 * outputs the result (will be a java ArrayList of 'Path|Metric's ) 
 * Usage: java -cp . ExampleMetricQueryClient "http://localhost:8800/\$3-69-ARBN/metrics/query?format=java&paths=Agent(3-69-ARBN):SpokeTime|IpFlow(blatz,stout):CapacityMax"
 * Must specify 'format=java' as the default is a string of xml printed out to browser
 */
package org.cougaar.core.examples.metrics;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// This example requires quoSumo.jar, qos.jar and core.jar
public class ExampleMetricQueryClient
{
  // One arg here - the query string "http://host..." see usage above
  public static void main( String args[] ) throws IOException
  {
    String url = null;
    try {
      url=args[0];
      URL servlet_url = new URL(url);
      
      // open up input stream and read object
      try {
	// open url connection from string url
	InputStream in = servlet_url.openStream();
	ObjectInputStream ois = new ObjectInputStream(in);
	
	// read in java object - this a java client only
	HashMap propertylist = (HashMap)ois.readObject();
	ois.close();

	if (propertylist == null) {
	    System.out.println("Null Property List returned");
	    return;
	}
	// can do anything with it here, we just print it out for now
	Iterator itr = propertylist.entrySet().iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    System.out.println(entry.getKey() +"->"+ entry.getValue());
	}
      } catch (Exception e) {
	System.out.println("Error reading input stream for url " + url + " Make sure the stream is open. " + e);
	e.printStackTrace();
      }
    } catch(Exception e){
      System.out.println("Unable to acquire URL, Exception: " + e);
    }
  }  
}
