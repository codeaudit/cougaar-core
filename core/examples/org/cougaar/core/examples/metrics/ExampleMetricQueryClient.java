/*
 * Stand alone java client that opens up a URL connection, reads an object stream and 
 * outputs the result (will be a java ArrayList of 'Path|Metric's ) 
 * Usage: java -cp . ExampleMetricQueryClient "http://localhost:8800/\$3-69-ARBN/metrics/query?format=java&paths=Agent(3-69-ARBN):SpokeTime|IpFlow(blatz,stout):CapacityMax"
 * Must specify 'format=java' as the default is a string of xml printed out to browser
 */

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

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
	ArrayList propertylist = (ArrayList)ois.readObject();
	// can do anything with it here, we just print it out for now
	System.out.println(propertylist);
      } catch (Exception e) {
	System.out.println("Error reading input stream for url " + url + " Make sure the stream is open. " + e);
	e.printStackTrace();
      }
    } catch(Exception e){
      System.out.println("Unable to acquire URL, Exception: " + e);
    }
  }  
}
