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

package org.cougaar.core.examples.metrics;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.qos.metrics.Metric;

/**
 * Standalone java client that opens a URL connection, reads an object stream and 
 * prints the result.
 * <p> The URL should be a metric query directed at the MetricsQueryServlet for
 * some agent.  The object in the reply stream will of type 
 * <code>Map&lt;String, Metric&gt;</code>, where
 * the keys are the paths in the query portion of the URL:
 * <pre>
 * Usage: java -cp . ExampleMetricQueryClient "http://localhost:8800/\$3-69-ARBN/metrics/query?format=java&paths=Agent(3-69-ARBN):SpokeTime|IpFlow(blatz,stout):CapacityMax"
 *</pre>
 *<p> You must specify 'format=java' to get a Map back, since the default behavior is to print
 * a string of xml to a web browser.
 * <p>
 * This example requires qos.jar and qrs.jar 
 */
public class ExampleMetricQueryClient {
    private static final String DefaultQuery = 
       " http://localhost:8800/$/metrics/query?format=java&paths=$(localhost):EffectiveMJips|$(localhost):LoadAverage";
    // One arg here - the query string "http://host..." see usage above
    public static void main(String args[]) {
        String url = null;
        try {
            url = args.length > 0 ? args[0] : DefaultQuery;
            URL servlet_url = new URL(url);

            // open up input stream and read object
            try {
                // open url connection from string url
                InputStream in = servlet_url.openStream();
                ObjectInputStream ois = new ObjectInputStream(in);

                // read in java object - this a java client only
                @SuppressWarnings("unchecked")
                Map<String, Metric> propertylist = (Map<String, Metric>) ois.readObject();
                ois.close();

                if (propertylist == null) {
                    System.out.println("Null Property List returned");
                    return;
                }
                Set<Map.Entry<String, Metric>> entries = propertylist.entrySet();
                // can do anything with it here, we just print it out for now
                for (Map.Entry<String, Metric> entry : entries) {
                    System.out.println(entry.getKey() + "->" + entry.getValue());
                }
            } catch (Exception e) {
                System.out.println("Error reading input stream for url " + url
                        + " Make sure the stream is open. " + e);
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Unable to acquire URL, Exception: " + e);
        }
    }
}
