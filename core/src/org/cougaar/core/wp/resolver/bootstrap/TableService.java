/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.wp.resolver.bootstrap;

import java.util.*;
import org.cougaar.core.component.Service;
import org.cougaar.core.service.wp.AddressEntry;

/**
 * A service for bootstrap components, such as config
 * readers and bootstrap lookup components.
 * <p>
 * Only child components of Resolver can obtain this service.
 */
public interface TableService
extends Service 
{

  /** lookup an entry */
  AddressEntry get(String name, String type, String scheme);

  /** iterate over entries */
  Iterator iterator();

  /**
   * Modify the bootstrap table.
   * <p>
   * The changes are visible when WP cache miss occurs, which may be
   * delayed by a TTL from a non-bootstrap response.  For example,
   * if the WP authority tells the resolver that "agent x is 
   * on foo.com", the cache will use this entry until it expires,
   * even if the bootstrap table indicates otherwise.
   */
  void add(AddressEntry entry);
  void replace(AddressEntry oldE, AddressEntry newE);
  void remove(AddressEntry entry);

  /**
   * Add a watcher for the table or the local WP bind actions.
   */
  void register(Watcher w);
  void unregister(Watcher w);

  /**
   * A generic bootstrap table watcher, as defined in
   * the subclasses below.
   */
  interface Watcher {
  }

  /**
   * Watch add/remove of bootstrap table entries.
   * <p>
   * This is useful for additional lookup processing, e.g.
   * a component that translates RMI registry names:
   *   "rmi://foo.com:123/AgentX"
   * to their runtime encoded stubs:
   *   Remote rem = Naming.lookup("rmi://foo.com:123/AgentX");
   *   AddressEntry entry = ((MyRemoteObj) rem).getAddressEntry();
   * where the entry's URI may look like:
   *   "rmi://123.45.67.89:9876/Obj/13_57_9"
   * to match an encoded MTS RMILinkProtocol stub object.  This
   * allows a client to bootstrap with the RMI symbolic name
   * instead of the runtime-determined stub object ID.
   */
  interface TableWatcher extends Watcher {
    void init(List entries);
    void added(AddressEntry entry);
    void removed(AddressEntry entry);
  }

  /**
   * Watch the local WP client *bind activity.
   * <p>
   * This watches the outgoing requests, not the responses.
   * <p>
   * This is intended to work with the above "TableWatcher",
   * e.g. when a client does a
   *   "bind AgentX rmi://123.45.67.89:9876/Obj/13_57_9"
   * a component could advertise this in an RMI registry:
   *   Registry reg = LocateRegistry.getRegistry("foo.com", 123);
   *   RemoteObj robj = new MyRemoteObj("rmi://123.45..");
   *   reg.bind("AgentX", robj);
   * As seen in the TableWatcher example above, this can be
   * used to bootstrap an RMI-based MTS.
   */
  interface BindWatcher extends Watcher {
    void bind(AddressEntry entry);
    void rebind(AddressEntry entry);
    void unbind(AddressEntry entry);
  }
}
