package org.cougaar.core.adaptivity;
/** 
 * published to the blackboard by a plugin in order
 * to send an object to a destination by name
 * This will be picked up by a logic provider which will
 * create a real message and call the Message Transport Service
 * to send it.
 */

public class TransferRequest {

  private String destination;
  private Object contents;

  /**
   * Constructor
   * @param String 'to' destination/agent
   * @param Object contents of the message
  public TransferRequest(String to,  Object messageContents){
    destination = to;
    contents = messageContents;
  }

  /**
   * The destination agent.
   * @return String identifying the agent
   */
  public String getDestination() {
    return destination;
  }
  /**
   * The message content.
   * @ Object containing the contents of the message.
   */
  public Object getContents(){
    return contents;
  }

}



