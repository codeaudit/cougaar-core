/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.naming;

import junit.framework.*;

public class GlobTest extends TestCase {
    private static String[] testPatterns = {
        "*",  		// Anything
        "a*",           // Any starting with a
        "*a",           // Any ending with a
        "*a*",          // Any containing a
        "c*a",          // Any starting with c and ending with a
        "cb*a",         // Any starting with cb and ending with a
    };

    private static class Case {
        String outcome;
        String testString;
        Case(String outcome, String testString) {
            this.outcome = outcome;
            this.testString = testString;
        }
    }

    private static Case[] testCases = {
        new Case("x     ", ""),
        new Case("xxxx  ", "a"),
        new Case("xx x  ", "aabc"),
        new Case("xx x  ", "abac"),
        new Case("xx x  ", "abc"),
        new Case("xxxx  ", "abca"),
        new Case("x     ", "b"),
        new Case("x  x  ", "baac"),
        new Case("x xx  ", "baca"),
        new Case("x  x  ", "cab"),
        new Case("x xxxx", "cba"),
        new Case("x xxxx", "cbaa"),
        new Case("x xxxx", "cbxyzaa"),
        new Case("x     ", "def"),
    };

    public GlobTest(String name) {
        super(name);
    }

    public void testOne() {
        for (int i = 0; i < testPatterns.length; i++) {
            Glob pattern = Glob.parse(testPatterns[i]);
            for (int j = 0; j < testCases.length; j++) {
                Case testCase = testCases[j];
                boolean expected = testCase.outcome.charAt(i) == 'x';
                if (pattern.match(testCase.testString)) {
                    if (!expected) {
                        fail("Unexpected match of " + testCase.testString + " by " + testPatterns[i]);
                    }
                } else {
                    if (expected) {
                        fail("Failed match of " + testCase.testString + " by " + testPatterns[i]);
                    }
                }
            }
        }
    }
}
