/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingException;

public class SearchStringParser {
    private static final String LP = "(";
    private static final String RP = ")";
    private static final String AND = "&";
    private static final String OR = "|";
    private static final String NOT = "!";
    private static final String SEPS = LP + RP + AND + OR + NOT;

    private StringTokenizer tokens;
    private String token;
    private String peek;        // Lookahead token

    public static class ParseException extends NamingException {
        public ParseException(String msg) {
            super(msg);
        }
    }

    public Filter parse(String s) throws ParseException {
        tokens = new StringTokenizer(s, SEPS, true);
        return filter();
    }

    private String getToken() throws ParseException {
        if (peek != null) {
            token = peek;
            peek = null;
        } else {
            if (!tokens.hasMoreTokens()) parseError("premature end");
            token = tokens.nextToken();
        }
        return token;
    }

    private String peekToken() throws ParseException {
        peek = getToken();
        return peek;
    }

    private void checkToken(String expected) throws ParseException {
        if (!expected.equals(getToken())) parseError(expected + " missing");
    }

    private void parseError(String why) throws ParseException {
        throw new ParseException(why);
    }

    private Filter filter() throws ParseException {
        checkToken(LP);
        Filter result = filtercomp();
        checkToken(RP);
        return result;
    }

    private Filter filtercomp() throws ParseException {
        getToken();
        if (token.equals(AND)) return new FilterAnd(filterlist());
        if (token.equals(OR)) return new FilterOr(filterlist());
        if (token.equals(NOT)) return new FilterNot(filter());
        if (token.equals(LP)) parseError(LP + " unexpected");
        if (token.equals(RP)) parseError(RP + " unexpected");
        return item(token);
    }

    private Filter[] filterlist() throws ParseException {
        List result = new ArrayList();
        do {
            result.add(filter());
        } while (!RP.equals(peekToken()));
        return (Filter[]) result.toArray(new Filter[result.size()]);
    }

    /**
     * An item is an attribute description, filtertype, and pattern or
     * value. All filtertypes have an equal sign, so we look for that.
     * Then we check the character preceding the equal sign to see if
     * it is also part of the filter type.
     **/
    private Filter item(String s) throws ParseException {
        int eqPos = s.indexOf('=');
        if (eqPos < 1) parseError("filtertype missing");
        String pattern = s.substring(eqPos + 1);
        switch (s.charAt(eqPos - 1)) {
        case '<':
            return new FilterLessThan(s.substring(0, eqPos - 1), pattern);
        case '>':
            return new FilterGreaterThan(s.substring(0, eqPos - 1), pattern);
        case '~':
            return new FilterApproximateMatch(s.substring(0, eqPos - 1), pattern);
        case ':':
            parseError("matching rules not supported");
        default:
            String attr = s.substring(0, eqPos);
            if (pattern.indexOf('*') < 0) {
                return new FilterEquality(attr, pattern);
            } else if (pattern.length() == 1) {
                return new FilterPresence(attr);
            } else {
                return new FilterSubstring(attr, pattern);
            }
        }
    }

    private static class FilterAnd implements Filter {
        private Filter[] list;

        public FilterAnd(Filter[] list) {
            this.list = list;
        }

        public boolean match(Attributes attrs) throws NamingException {
            for (int i = 0; i < list.length; i++) {
                if (!list[i].match(attrs)) return false;
            }
            return true;
        }
    }

    private static class FilterOr implements Filter {
        private Filter[] list;

        public FilterOr(Filter[] list) {
            this.list = list;
        }

        public boolean match(Attributes attrs) throws NamingException {
            for (int i = 0; i < list.length; i++) {
                if (list[i].match(attrs)) return true;
            }
            return false;
        }
    }

    private static class FilterNot implements Filter {
        private Filter filter;

        public FilterNot(Filter filter) {
            this.filter = filter;
        }

        public boolean match(Attributes attrs) throws NamingException {
            return !filter.match(attrs);
        }
    }

    private static class FilterLessThan implements Filter {
        private String attrdesc, value;

        public FilterLessThan(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return value.compareTo(attrs.get(attrdesc).get()) <= 0;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class FilterGreaterThan implements Filter {
        private String attrdesc, value;

        public FilterGreaterThan(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return value.compareTo(attrs.get(attrdesc).get()) >= 0;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class FilterPresence implements Filter {
        private String attrdesc;

        public FilterPresence(String attrdesc) {
            this.attrdesc = attrdesc;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return attrs.get(attrdesc) != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class FilterEquality implements Filter {
        private String attrdesc, value;

        public FilterEquality(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.value = value;
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return value.equals(attrs.get(attrdesc).get());
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class FilterSubstring implements Filter {
        private String attrdesc;
        private Glob glob;

        public FilterSubstring(String attrdesc, String value) {
            this.attrdesc = attrdesc;
            this.glob = Glob.parse(value);
        }

        public boolean match(Attributes attrs) throws NamingException {
            try {
                return glob.match(attrs.get(attrdesc).get().toString());
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static class FilterApproximateMatch extends FilterEquality {
        public FilterApproximateMatch(String attrdesc, String value) {
            super(attrdesc, value);
        }
    }
}
