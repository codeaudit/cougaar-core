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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingException;
import junit.framework.*;

public class SearchStringParserTest extends TestCase {

    private static final String t01 = "(attr1=abcd)";         // Simple eq
    private static final String t02 = "(attr2<=abcd)";        // Simple le
    private static final String t03 = "(attr3>=abcd)";        // Simple ge
    private static final String t04 = "(attr4~=abcd)";        // Simple like
    private static final String t05 = "(attr5=*)";            // Simple presence
    private static final String t06 = "(attr6=ab*cd)";        // Simple substring
    private static final String t07 = "(& " + t01 + "" + t02 + ")";
    private static final String t08 = "(| " + t03 + "" + t04 + ")";
    private static final String t09 = "(! " + t05 + ")";
    private static final String t10 = "(& " + t08 + " " + t02 + ")";
    private static final String t11 = "(| " + t07 + " " + t10 + ")";
    private static final String t12 = "(! " + t11 + ")";

    private SearchStringParser parser;

    public SearchStringParserTest(String name) {
        super(name);
    }

    protected void setUp() {
        parser = new SearchStringParser();
    }

    protected void tearDown() {
        parser = null;
    }

    public void test01() { oneTest(t01); }
    public void test02() { oneTest(t02); }
    public void test03() { oneTest(t03); }
    public void test04() { oneTest(t04); }
    public void test05() { oneTest(t05); }
    public void test06() { oneTest(t06); }
    public void test07() { oneTest(t07); }
    public void test08() { oneTest(t08); }
    public void test09() { oneTest(t09); }
    public void test10() { oneTest(t10); }
    public void test11() { oneTest(t11); }
    public void test12() { oneTest(t12); }

    private String removeSpaces(String s) {
        int pos = 0;
        while ((pos = s.indexOf(' ', pos)) >= 0) {
            s = s.substring(0, pos) + s.substring(pos + 1);
        }
        return s;
    }

    private void oneTest(String test) {
        try {
            Filter filter = parser.parse(test);
            assertEquals("Parse error",
                         removeSpaces(test),
                         removeSpaces(filter.toString()));
        } catch (SearchStringParser.ParseException pe) {
            fail("Exception parsing " + test);
        }
    }
}
