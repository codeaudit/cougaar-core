/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package org.cougaar.core.persist;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * PersistencePlugin defines the API that media-specific persistence
 * plugins must implement. A persistence plugin defines a medium that
 * can be used to store a series of persistence snapshots. When an
 * agent restarts, a set of these snapshots called a rehydration set
 * is retrieved from the persistence medium to reconstitute or
 * "rehydrate" the previous state of the agent.
 **/
public interface PersistencePlugin {

    /**
     * Initialize the plugin. After initialization, the plugin should
     * be ready to service all methods.
     * @param pps the persistence plugin support specifies the context
     * within which persistence is being performed.
     **/
    void init(PersistencePluginSupport pps) throws PersistenceException;

    /**
     * Read the specified set of sequence numbers. These numbers
     * should identify a complete set of persistence deltas needed to
     * restore the specified state. A specific archive may be
     * specified using the suffix argument.
     * @return an array of possible rehydration sets. The timestamp of
     * each indicates how recent each rehydration set is.
     * @param suffix identifies which set of persistence deltas are
     * wanted. A non-empty suffix specifies an specific, archived
     * state. An empty suffix specifies all available sets.
     **/
    SequenceNumbers[] readSequenceNumbers(String suffix);

    /**
     * Cleanup old deltas as specified by cleanupNumbers. These deltas
     * are <em>never</em> part of the current state. When archiving is
     * enabled, the old deltas constituting an archive are not
     * discarded.
     * @param cleanupNumbers the numbers to be discarded (or archived).
     **/
    void cleanupOldDeltas(SequenceNumbers cleanupNumbers);

    /**
     * Open an ObjectOutputStream onto which a persistence delta can
     * be written. The stream returned should be relatively
     * non-blocking since it is possible for the entire agent to be
     * blocked waiting for completion. Implementations that may block
     * indefinitely should perform buffering as needed.
     * @param deltaNumber the number of the delta that will be
     * written. Numbers are never re-used so this number can be used
     * to uniquely identify the delta.
     * @param full indicates that the information to be written is a
     * complete state dump and does not depend on any earlier deltas.
     * It may be useful to distinctively mark such deltas.
     **/
    ObjectOutputStream openObjectOutputStream(int deltaNumber, boolean full)
        throws IOException;

    /**
     * Abort output. The partially written stream should be discarded
     * and artifacts of its existence eliminated. Later, a write of
     * the same delta may be attempted probably with different data.
     * Called in response to an exception during the writing of the
     * current stream.
     * @param retainNumbers the numbers of the deltas excluding the
     * one just written that comprise a complete rehydration set.
     * Subsequent calls to readSequenceNumbers should return these
     * values.
     * @param currentOutput always the most recently opened stream
     * provided as a convenience so it need not be retained.
     **/
    void abortObjectOutputStream(SequenceNumbers retainNumbers,
                                 ObjectOutputStream currentOutput);

    /**
     * Close the output stream. Should reliably terminate the writing
     * of the stream. Implementations should attempt to insure that
     * the written data will not be lost, but the non-blocking
     * requirement above must be taken into consideration.
     * @param retainNumbers the numbers of the deltas including the
     * one just written that comprise a complete rehydration set.
     * Subsequent calls to readSequenceNumbers should return these
     * values.
     * @param currentOutput always the most recently opened stream
     * provided as a convenience so it need not be retained.
     **/
    void closeObjectOutputStream(SequenceNumbers retainNumbers,
                                 ObjectOutputStream currentOutput,
                                 boolean full);

    /**
     * Open an ObjectInputStream from which a persistence delta can be
     * read.
     * @param deltaNumber the number of the delta to be opened
     **/
    ObjectInputStream openObjectInputStream(int deltaNumber)
        throws IOException;

    /**
     * Close the ObjectInputStream.
     * @param deltaNumber the number of the delta being closed.
     * Provided as a convenience to the method
     * @param currentInput the ObjectInputStream being closed.
     * Provided as a convenience to the method.
     **/
    void closeObjectInputStream(int deltaNumber,
                                ObjectInputStream currentInput);

    /**
     * Clean out all old persistence information. Intended only as a
     * debugging aid to facilitate startup without any previous
     * persisted state. Generally, the media should be wiped clean of
     * all previous persistence state.
     **/
    void deleteOldPersistence();

    /**
     * Get the connection to the database into which persistence
     * deltas are being written for coordinated transaction
     * management. Non-database implementations should throw an
     * UnsupportedOperationException
     **/
    java.sql.Connection getDatabaseConnection(Object locker) throws UnsupportedOperationException;

    /**
     * Release the connection to the database into which persistence
     * deltas are being written for coordinated transaction
     * management. Non-database implementations should throw an
     * UnsupportedOperationException
     **/
    void releaseDatabaseConnection(Object locker) throws UnsupportedOperationException;
}
