package org.cougaar.core.naming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Matches unix-style glob patterns
 **/
public class Glob {
    /** The parsed segments of the pattern **/
    private Segment[] segments;

    /** A cache of already parsed patterns **/
    private static Map globs = new HashMap();

    /**
     * Parse a glob pattern or find the previously parsed result. In
     * glob patterns:
     * <dl>
     * <dt>*</dt><dd>matches any number of any character</dd>
     * <dt>?</dt><dd>matches a single character</dd>
     * <dt>[...]</dt><dd> Matches any single character in the enclosed
     * list(s) or range(s). A list is a string of characters. A range
     * is two characters separated by a dash (-), and includes all the
     * characters in between in the collating sequence of the default
     * charset. To include a ']' in the set, put it first. To include
     * a '-', put it first or last.</dd>
     * </dl>
     * @param pattern the pattern to parse.
     * @return the parsed result. The match method of the returned
     * object should be used to check if any given string matches the
     * pattern that was parsed.
     **/
    public static synchronized Glob parse(String pattern) {
        Glob result = (Glob) globs.get(pattern);
        if (result == null) {
            result = new Glob(pattern);
            globs.put(pattern, result);
        }
        return result;
    }

    /**
     * Construct a new Glob to parse a pattern. The constructor is
     * private. Use the parse method to create or reuse a Glob. The
     * parsed result consists of an array of Segments where each
     * Segment corresponds to a portion of the pattern. There are
     * four kinds of Segments corresponding "*", "?", "[...]" and
     * ordinary characters.
     **/
    private Glob(String s) {
        List segmentList = new ArrayList();
        int startPos = 0;
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '*':
                if (startPos < i) {
                    segmentList.add(new Match(s.substring(startPos, i)));
                }
                segmentList.add(new Star());
                startPos = i + 1;
                break;
            case '?':
                if (startPos < i) {
                    segmentList.add(new Match(s.substring(startPos, i)));
                }
                segmentList.add(new AnyOne());
                startPos = i + 1;
                break;
            case '[':
                if (startPos < i) {
                    segmentList.add(new Match(s.substring(startPos, i)));
                }
                CharSet charSet = new CharSet();
                c = s.charAt(++i);
                if (c == '!') {
                    charSet.negate = true;
                    c = s.charAt(++i);
                }
                boolean first = true;
                boolean dash = false;
                char pc = c;
                while (first || c != ']') { // ] must be first if at all
                    if (!first && c == '-') {
                        c = s.charAt(++i);
                        if (c == ']') {
                            charSet.add('-');
                            break;
                        }
                        while (++pc <= c) charSet.add(pc);
                    } else {
                        charSet.add(c);
                        pc = c;
                    }
                    c = s.charAt(++i);
                    first = false;
                }
                segmentList.add(charSet.finish());
                startPos = i + 1;
            }
        }
        if (startPos < s.length()) {
            segmentList.add(new Match(s.substring(startPos)));
        }
        segments = (Segment[]) segmentList.toArray(new Segment[segmentList.size()]);
        int off = 0;            // The min chars needed for all remaining segments
        for (int i = segments.length; --i >= 0; ) {
            off += segments[i].getMin();
            segments[i].setOff(off);
        }
//          for (int i =0; i < segments.length; i++) {
//              System.out.println(segments[i].getClass().getName()
//                                 + ": "
//                                 + segments[i]);
//          }
    }

    /**
     * Check if a string matches the pattern. The procedure is to step
     * through the segments in sequence and see if the segment matches
     * the beginning of the current string. If it doesn't return false
     * (failure). If it does, trim off the matched characters and
     * repeat for the next segment. Each segment specifies the minimum
     * and maximum characters that it can match. If the maximum
     * exceeds the minimum, all possibilities are tested starting with
     * the longest (maximum number of character) until a match is
     * achieved. If all the possibilities fail, then this method
     * fails.
     * @param s the string to test
     **/
    public boolean match(String s) {
        return match(s, 0);
    }

    /**
     * Check if a string matches the pattern starting with a
     * particular segment.
     * @see match(String s)
     * @param s the string to test
     * @param ix the index of the segment to start with (recursive
     * call).
     **/
    private boolean match(String s, int ix) {
        for (int n = segments.length; ix < n; ) {
            Segment seg = segments[ix];
            int off = seg.getOff();
            if (off > s.length()) {
                return false;       // Not enough chars left
            }
            if (!seg.match(s)) return false;
            int min = seg.getMin();
            int nextOff = off - min;
            int max = Math.min(seg.getMax(), s.length() - nextOff);
            for (int pos = max; pos > min; pos--) {
                if (match(s.substring(pos), ix + 1)) {
                    return true;
                }
            }
            s = s.substring(min); // Tail recursion
            ix = ix + 1;
        }
        return s.length() == 0;
    }

    /**
     * Convert the parsed pattern to a string
     **/
    public String toString() {
        return appendString(new StringBuffer()).toString();
    }

    public StringBuffer appendString(StringBuffer buf) {
        for (int i = 0; i < segments.length; i++) {
            buf.append(segments[i].toString());
        }
        return buf;
    }

    public interface Segment {
        void setOff(int newOff);
        int getOff();
        int getMin(); // The minimum length of this segment
        int getMax(); // The maximum length of this segment
        boolean match(String s);
    }

    private static class SegmentBase {
        private int min;
        private int max;
        private int off;
        protected SegmentBase(int min, int max) {
            this.min = min;
            this.max = max;
        }
        public final void setOff(int newOff) {
            off = newOff;
        }
        public final int getOff() {
            return off;
        }
        public final int getMax() {
            return max;
        }
        public final int getMin() {
            return min;
        }
    }
    private static class Match extends SegmentBase implements Segment {
        String chars;
        public Match(String s) {
            super(s.length(), s.length());
            chars = s;
        }
        public boolean match(String s) {
            return s.startsWith(chars);
        }
        public String toString() {
            return chars;
        }
    }

    private static class Star extends SegmentBase implements Segment {
        public Star() {
            super(0, Integer.MAX_VALUE);
        }
        public boolean match(String s) {
            return true;
        }
        public String toString() {
            return "*";
        }
    }

    private static class One extends SegmentBase {
        public One() {
            super(1, 1);
        }
    }

    private static class AnyOne extends One implements Segment {
        public boolean match(String s) {
            return true;
        }
        public String toString() {
            return "?";
        }
    }

    private static class CharSet extends One implements Segment {
        public boolean negate = false;
        private StringBuffer buf = new StringBuffer();
        private String chars = null;
        public void add(char c) {
            buf.append(c);
        }
        public CharSet finish() {
            chars = buf.substring(0);
            buf = null;
            return this;
        }
        public boolean match(String s) {
            char c = s.charAt(0);
            return (chars.indexOf(c) >= 0) != negate;
        }
        public String toString() {
            StringBuffer b = new StringBuffer();
            b.append("[");
            if (negate) b.append("!");
            if (chars.indexOf(']') >= 0) b.append(']');
            int charsInRange = 0;
            int nextInRange = -1;
            for (int i = 0, n = chars.length(); i <= n; i++) {
                int c;
                if (i < n) {
                    c = chars.charAt(i);
                } else {
                    c = -1;
                }
                if (c == '-') continue;
                if (c == ']') continue;
                if (charsInRange > 0 && c == nextInRange) {
                    charsInRange++;
                } else {
                    if (charsInRange > 2) b.append('-');
                    if (charsInRange > 1) b.append((char) (nextInRange - 1));
                    if (i < n) {
                        b.append((char) c);
                        charsInRange = 1;
                    }
                }
                nextInRange = c + 1;
            }
            if (chars.indexOf('-') >= 0) b.append('-');
            b.append("]");
            return b.toString();
        }
    }

    private static String[] testStrings = {
        "",
        "a",
        "aabc",
        "abac",
        "abc",
        "abca",
        "b",
        "baac",
        "baca",
        "cab",
        "cba",
        "cbaa",
        "cbxyzaa",
        "def",
    };

    private static String[] testPatterns = {
        "*",  		// Anything
        "a*",           // Any starting with a
        "*a",           // Any ending with a
        "*a*",          // Any containing a
        "c*a",          // Any starting with c and ending with a
        "cb*a",         // Any starting with cb and ending with a
    };

    private static final String chars = "abcde";

    private static Random r = new Random();

    private static String randomString(int maxlen) {
        int len = r.nextInt(maxlen + 1);
        char[] result = new char[len];
        for (int i = 0; i < len; i++) {
            result[i] = chars.charAt(r.nextInt(chars.length()));
        }
        return new String(result);
    }

    private static String[] getTestStrings(int n) {
        Set did = new HashSet();
        String[] result = new String[n];
        for (int i = 0; i < n; i++) {
            if (i < testStrings.length) {
                did.add(testStrings[i]);
                result[i] = testStrings[i];
            } else {
                String s;
                do {
                    s = randomString(5);
                } while (did.contains(s));
                result[i] = s;
            }
        }
        return result;
    }

    private static void test(String[] patterns, String[] strings) {
        for (int i = 0; i < patterns.length; i++) {
            Glob pattern = Glob.parse(patterns[i]);
            if (i > 0 && strings.length > 1) System.out.println();
            for (int k = 0; k < 2; k++) {
                boolean match = k == 0;
                for (int j = 0; j < strings.length; j++) {
                    String string = strings[j];
                    if (match == pattern.match(string)) {
                        System.out.println(pattern + "(" + string + ")=" + match);
                    }
                }
            }
        }
    }

    public static void testmain(String args[]) {
        if (args.length > 2) {
            String[] patterns = new String[] {args[0]};
            String[] strings = new String[args.length - 1];
            System.arraycopy(args, 1, strings, 0, strings.length);
            test(patterns, strings);
        } else if (args.length > 0) {
            test(new String[] {args[0]}, getTestStrings(50));
        } else {
            test(testPatterns, getTestStrings(50));
        }
    }

    public static void main(String args[]) {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i] + " -> " +
                               parse(args[i]).toString());
        }
    }
}
