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
    private Segment[] segments;

    private static Map globs = new HashMap();

    public static synchronized Glob parse(String s) {
        Glob result = (Glob) globs.get(s);
        if (result == null) {
            result = new Glob(s);
            globs.put(s, result);
        }
        return result;
    }

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
                CharSet charSet = new CharSet();
                c = s.charAt(++i);
                if (c == '!') {
                    charSet.negate = true;
                    c = s.charAt(++i);
                }
                boolean first = true;
                while (first || c != ']') {
                    first = false;
                    char c2 = s.charAt(++i);
                    if (c2 == '-') {
                        c2 = s.charAt(++i);
                        charSet.add(c, c2);
                        c = s.charAt(++i);
                    } else {
                        charSet.add(c, c);
                        c = c2;
                    }
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
    }

    public void setSegments(Segment[] newSegments) {
        segments = newSegments;
    }

    public Segment[] getSegments() {
        return segments;
    }

    public boolean match(String s) {
        return match(s, 0);
    }
    
    private boolean match(String s, int ix) {
        boolean result = match1(s, ix);
        return result;
    }
    private boolean match1(String s, int ix) {
        if (ix == segments.length) {
            if (s.length() == 0) {
                return true;
            } else {
//                  System.out.println("long " + s);
                return false;
            }
        }
        Segment seg = segments[ix];
        int off = seg.getOff();
        if (off > s.length()) {
//              System.out.println("short " + off);
            return false;       // Not enough chars left
        }
        if (seg.match(s)) {
            int min = seg.getMin();
            int nextOff = off - min;
            int max = Math.min(seg.getMax(), s.length() - nextOff);
            for (int pos = max; pos >= min; pos--) {
                if (match(s.substring(pos), ix + 1)) {
                    return true;
                }
            }
//              System.out.println("offsets " + min + ".." + max + " fail");
        } else {
//              System.out.println(seg + "!=" + s);
        }
        return false;
    }

    public String toString() {
        return appendString(new StringBuffer()).toString();
    }

    protected StringBuffer appendString(StringBuffer buf) {
        for (int i = 0; i < segments.length; i++) {
            buf.append(segments[i].toString());
        }
        return buf;
    }

    public static interface Segment {
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
        public void add(char c1, char c2) {
            while (c1 <= c2) {
                buf.append(c1++);
            }
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
            if (negate) {
                return "[!" + chars + "]";
            } else {
                return "[" + chars + "]";
            }
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

    public static void main(String args[]) {
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
}
