package org.cougaar.core.adaptivity;
import java.util.*;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.FileInputStream;
import java.io.StringBufferInputStream;

public class Parser {
  StreamTokenizer st;
  boolean pushedBack = false;
  ConstrainingClause cc;

  public Parser(StreamTokenizer s) {
    st = s;
    st.wordChars('_', '_');
  }

  public ConstrainingClause parseConstrainingClause() throws IOException {
    cc = new ConstrainingClause();
    parseExpression();
    ConstrainingClause result = cc;
    cc = null;
    return result;
  }

  public ConstraintPhrase[] parseConstraints() throws IOException {
    List constraintPhrases = new ArrayList();
    while (true) {
      int token = nextToken();
      if (token == StreamTokenizer.TT_EOF) break;
      if (token == ';') {
        break;
      }
      if (token != ':') throw unexpectedTokenException(1);
      constraintPhrases.add(parseConstraintPhrase());
    }

    return (ConstraintPhrase[]) constraintPhrases.toArray(new ConstraintPhrase[constraintPhrases.size()]);
  }

  /**
   **/
  public Play parsePlay() throws IOException {
    ConstrainingClause cc = parseConstrainingClause();          // Parse the ifClause
    ConstraintPhrase[] cp = parseConstraints();
    return new Play(cc, cp);
  }

  public Play[] parsePlays() throws IOException {
    List plays = new ArrayList();
    while (true) {
      plays.add(parsePlay());
      if (nextToken() == StreamTokenizer.TT_EOF) break;
      pushBack();
    }
    return (Play[]) plays.toArray(new Play[plays.size()]);
  }

  /**
   **/
  public OperatingModePolicy parseOperatingModePolicy() throws IOException {
    ConstrainingClause cc = parseConstrainingClause();          // Parse the ifClause
    ConstraintPhrase[] cp = parseConstraints();
    return new OperatingModePolicy(cc, cp);
  }

  public OperatingModePolicy[] parseOperatingModePolicies() throws IOException {
    List policies = new ArrayList();
    while (true) {
      policies.add(parseOperatingModePolicy());
      if (nextToken() == StreamTokenizer.TT_EOF) break;
      pushBack();
    }
    return (OperatingModePolicy[]) policies.toArray(new OperatingModePolicy[policies.size()]);
  }
      
  /**
   * Sensor, boolean exp, identifier, operator, value
   */
  private void parseExpression() throws IOException {
    parseTerm();
    int token = nextToken();
    switch (token) {
    default:
      pushBack();
      return;
    case '&':
        parseExpression();
        cc.pushOperator(BooleanOperator.AND);
        return;
    case '|':
        parseExpression();
        cc.pushOperator(BooleanOperator.OR);
        return;
    case '(':
      parseExpression();
      return;
    case ')':
      return;
    } 
  }
  
  private void parseTerm() throws IOException {
    int token = nextToken();
    switch (token) {
    case StreamTokenizer.TT_WORD:
      if (st.sval.equalsIgnoreCase(BooleanOperator.TRUE.toString())) {
        cc.pushOperator(BooleanOperator.TRUE);
        return;
      }
      if (st.sval.equalsIgnoreCase(BooleanOperator.FALSE.toString())) {
        cc.pushOperator(BooleanOperator.FALSE);
        return;
      }
      cc.pushPhrase(parseConstraintPhrase(st.sval));
      return;
    case '!':
      parseExpression();
      cc.pushOperator(BooleanOperator.NOT);
      return;
    case '(':
      parseExpression();
      return;
    default:
      throw unexpectedTokenException(2);
      
    } 
  }
  private ConstraintPhrase parseConstraintPhrase() throws IOException {
    if (nextToken() != StreamTokenizer.TT_WORD) throw unexpectedTokenException(3);
    return parseConstraintPhrase(st.sval);
  }

  private ConstraintPhrase parseConstraintPhrase(String name) throws IOException {
    int token1 = nextToken();
    ConstraintOperator op;
    OMSMValueList parameter;
    
    switch (token1) {
    case '<':
    case '>':
    case '=':
    case '!':
      int token2 = nextToken();
      if (token2 == '=') {
        switch (token1) {
        case '<':
          op = ConstraintOperator.LESSTHANOREQUAL;
          break;
        case '>':
          op = ConstraintOperator.GREATERTHANOREQUAL;
          break;
        case '=':
          op = ConstraintOperator.EQUAL;
          break;
        case '!':
          op = ConstraintOperator.NOTEQUAL;
          break;
        default:
          throw unexpectedTokenException(4);
        }
        token1 = nextToken();
      } else {
        switch (token1) {
        case '=':
          op = ConstraintOperator.ASSIGN;
          break;
        case '<':
          op = ConstraintOperator.LESSTHAN;
          break;
        case '>':
          op = ConstraintOperator.GREATERTHAN;
          break;
        default:
          throw unexpectedTokenException(5);
        }
        token1 = token2;
      }
      break; // end of punctuation chars case
    case StreamTokenizer.TT_WORD:
      if (st.sval.equalsIgnoreCase(ConstraintOperator.IN.toString())) {
        op = ConstraintOperator.IN;
        token1 = nextToken();
        break;
      }
      if (st.sval.equalsIgnoreCase("NOT")) {
        if (nextToken() == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("IN")) {
          op = ConstraintOperator.NOTIN;
          token1 = nextToken();
          break;
        }
      }
    default: 
      throw unexpectedTokenException(6);
    }
    if (token1 == StreamTokenizer.TT_WORD) {
      parameter = new OMSMValueList(parseRange(st.sval));
    } else if (token1 == StreamTokenizer.TT_NUMBER) {
      parameter = new OMSMValueList(parseRange(new Double(st.nval)));
    } else if (token1 == '{') {
      parameter = parseSet();
    } else {
      throw unexpectedTokenException(7);
    }
    return new ConstraintPhrase(name, op, parameter);
  }

  private OMSMRange parseRange(Comparable first) throws IOException {
    Class elementClass = first.getClass();
    int token = nextToken();
    if (token == StreamTokenizer.TT_WORD) {
      boolean isTo = st.sval.equalsIgnoreCase("to");
      boolean isThru = st.sval.equalsIgnoreCase("thru");
      if (isTo || isThru) {
        Comparable last;
        token = nextToken();
        if (token == StreamTokenizer.TT_WORD) {
          if (elementClass == Double.class) {
            throw new IllegalArgumentException("Values in range must have homogeneous types");
          }
          last = st.sval;
        } else if (token == StreamTokenizer.TT_NUMBER) {
          if (elementClass == String.class) {
            throw new IllegalArgumentException("Values in range must have homogeneous types");
          }
          last = new Double(st.nval);
        } else {
          throw unexpectedTokenException(8);
        }
        if (isTo) {
          return new OMSMToRange(first, last);
        } else {
          return new OMSMThruRange(first, last);
        }
      }
    }
    pushBack();
    return new OMSMPoint(first);
  }

  private OMSMValueList parseSet() throws IOException {
    Class elementClass = null;
    List values = new ArrayList();

    while (true) {
      int token1 = nextToken();
      if (token1 == StreamTokenizer.TT_WORD) {
        if (elementClass == Double.class) {
          throw new IllegalArgumentException("Values in set must have homogeneous types");
        } else if (elementClass == null) {
          elementClass = String.class;
        }
        values.add(parseRange(st.sval));
      } else if (token1 == StreamTokenizer.TT_NUMBER) {
        if (elementClass == String.class) {
          throw new IllegalArgumentException("Values in set must have homogeneous types");
        } else if (elementClass == null) {
          elementClass = Double.class;
        }
        values.add(parseRange(new Double(st.nval)));
      } else if (token1 == ',') {
        // ignore comma
      } else if (token1 == '}') {
        break;
      } else {
        throw unexpectedTokenException(11);
      }
    }
    return new OMSMValueList((OMSMRange[]) values.toArray(new OMSMRange[values.size()]));
  }

  private void pushBack() {
    pushedBack = true;
  }

  private int nextToken() throws IOException {
    int token;
    if (pushedBack) {
      pushedBack = false;
      token = st.ttype;
      System.out.println("repeat " + tokenAsString());
    } else {
      token = st.nextToken();
      System.out.println("token " + tokenAsString());
    }
    return token;
  }

  private String tokenAsString() {
    switch (st.ttype) {
    case StreamTokenizer.TT_WORD:
      return st.sval;
    case StreamTokenizer.TT_NUMBER:
      return String.valueOf(st.nval);
    case StreamTokenizer.TT_EOF:
      return "<eof>";
    default:
      return String.valueOf((char) st.ttype);
    }
  }

  private IllegalArgumentException unexpectedTokenException(int n) {
    String mesg = tokenAsString();
    IllegalArgumentException result =
      new IllegalArgumentException("Unexpected token(" + n + ") " + mesg);
    StackTraceElement[] trace = result.getStackTrace();
    StackTraceElement[] callerTrace = new StackTraceElement[trace.length - 1];
    System.arraycopy(trace, 1, callerTrace, 0, callerTrace.length);
    result.setStackTrace(callerTrace);
    return result;
  }
  
  public static void mai1n (String args []) {
    try {
      for (int i = 0; i < args.length; i++) {
        StringBufferInputStream sbis = new StringBufferInputStream(args[i]);
        StreamTokenizer st = new StreamTokenizer(sbis);
        Parser p = new Parser(st);
        ConstrainingClause cc = p.parseConstrainingClause();
        for (Iterator iter = cc.iterator(); iter.hasNext();) {
          System.out.println(iter.next());
        }
      }
    } catch (IOException io) {}
  }
  
  public static void main (String args []) {
    try {
      for (int i = 0; i < args.length; i++) {
        FileInputStream sbis = new FileInputStream(args[i]);
        StreamTokenizer st = new StreamTokenizer(sbis);
        Parser p = new Parser(st);
        Play[] plays = p.parsePlays();
        System.out.println(plays.length + " plays");
        for (int j = 0; j < plays.length; j++) {
          System.out.println(plays[j]);
        }
      }
    } catch (IOException io) {}
  }
}
