/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import java.util.*;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.FileReader;
import java.io.StringReader;

public class Parser {
  StreamTokenizer st;
  boolean pushedBack = false;
  ConstrainingClause cc;
  private static final boolean debug = true;

  public Parser(StreamTokenizer s) {
    st = s;
    st.wordChars('_', '_');
    st.wordChars('[', '[');
    st.wordChars(']', ']');
    st.ordinaryChars('/', '/');
    st.slashStarComments(true);
    st.slashSlashComments(true);
  }

  public ConstrainingClause parseConstrainingClause() throws IOException {
    cc = new ConstrainingClause();
    parse(Operator.MAXP);
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
      if (nextToken() == StreamTokenizer.TT_EOF) break;
      pushBack();
      policies.add(parseOperatingModePolicy());
    }
    return (OperatingModePolicy[]) policies.toArray(new OperatingModePolicy[policies.size()]);
  }
      
  /**
   * Parse an if clause and push it onto the ConstrainingClause.
   * @param lp left left precedence of operator calling this method.
   */
  private void parse(int lp) throws IOException {
    if (debug) {
      String caller = new Throwable().getStackTrace()[1].toString();
      System.out.println("Parse from " + caller + " " + lp);
    }
    try {
      int token = nextToken();
      switch (token) {
      default:
        throw unexpectedTokenException(2);
      case '!':
        parse(BooleanOperator.NOT.getLP());
        cc.push(BooleanOperator.NOT);
        break;
      case '-':
        parse(ArithmeticOperator.NEGATE.getLP());
        cc.push(ArithmeticOperator.NEGATE);
        break;
      case '(':
        parse(Operator.MAXP);
        token = nextToken();
        if (token != ')') throw unexpectedTokenException(3);
        break;
      case StreamTokenizer.TT_WORD:
        if (st.sval.equalsIgnoreCase(BooleanOperator.TRUE.toString())) {
          cc.push(BooleanOperator.TRUE);
          break;
        }
        if (st.sval.equalsIgnoreCase(BooleanOperator.FALSE.toString())) {
          cc.push(BooleanOperator.FALSE);
          break;
        }
        cc.push(st.sval);
        break;
      case StreamTokenizer.TT_NUMBER:
        cc.push(new Double(st.nval));
        break;
      }
      while (true) {
        token = nextToken();        // Operator
        if (token == ':') {
          pushBack();
          return;
        }
        Operator op;
        switch (token) {
        default: pushBack(); return;
        case StreamTokenizer.TT_WORD:
          if (!st.sval.equalsIgnoreCase("in") && !st.sval.equalsIgnoreCase("not")) {
            pushBack();
            return;
          }
          // Fall thru into parseConstraintOpValue
        case '<':
        case '>':
        case '=':
        case '!':
          if (ConstraintOperator.IN.getRP() >= lp) {
            // Not yet
            pushBack();
            return;
          }
          pushBack();
          cc.push(parseConstraintOpValue(null));
          return;
        case '&': op = BooleanOperator.AND; break;
        case '|': op = BooleanOperator.OR; return;
        case '+': op = ArithmeticOperator.ADD; break;
        case '-': op = ArithmeticOperator.SUBTRACT; break;
        case '*': op = ArithmeticOperator.MULTIPLY; break;
        case '/': op = ArithmeticOperator.DIVIDE; break;
        }
        int opp = op.getRP();
        if (opp < lp) {
          parse(opp);
          cc.push(op);
          continue;
        } else {
          pushBack();
          return;
        }
      }
    } finally {
      if (debug) System.out.println("Exit parse " + lp);
    }
  }

  private ConstraintPhrase parseConstraintPhrase() throws IOException {
    if (nextToken() != StreamTokenizer.TT_WORD) throw unexpectedTokenException(5);
    ConstraintPhrase cp = new ConstraintPhrase(st.sval);
    parseConstraintOpValue(cp);
    return cp;
  }

  private ConstraintOpValue parseConstraintOpValue(ConstraintOpValue cov) throws IOException {
    int token1 = nextToken();
    if (cov == null) cov = new ConstraintOpValue();
    
    switch (token1) {
    case '<':
    case '>':
    case '=':
    case '!':
      int token2 = nextToken();
      if (token2 == '=') {
        switch (token1) {
        case '<':
          cov.setOperator(ConstraintOperator.LESSTHANOREQUAL);
          break;
        case '>':
          cov.setOperator(ConstraintOperator.GREATERTHANOREQUAL);
          break;
        case '=':
          cov.setOperator(ConstraintOperator.EQUAL);
          break;
        case '!':
          cov.setOperator(ConstraintOperator.NOTEQUAL);
          break;
        default:
          throw unexpectedTokenException(6);
        }
        token1 = nextToken();
      } else {
        switch (token1) {
        case '=':
          cov.setOperator(ConstraintOperator.ASSIGN);
          break;
        case '<':
          cov.setOperator(ConstraintOperator.LESSTHAN);
          break;
        case '>':
          cov.setOperator(ConstraintOperator.GREATERTHAN);
          break;
        default:
          throw unexpectedTokenException(7);
        }
        token1 = token2;
      }
      break; // end of punctuation chars case
    case StreamTokenizer.TT_WORD:
      if (st.sval.equalsIgnoreCase(ConstraintOperator.IN.toString())) {
        cov.setOperator(ConstraintOperator.IN);
        token1 = nextToken();
        break;
      }
      if (st.sval.equalsIgnoreCase("NOT")) {
        if (nextToken() == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("IN")) {
          cov.setOperator(ConstraintOperator.NOTIN);
          token1 = nextToken();
          break;
        }
      }
    default: 
      throw unexpectedTokenException(6);
    }
    if (token1 == StreamTokenizer.TT_WORD) {
      cov.setAllowedValues(new OMCRangeList(parseRange(st.sval)));
    } else if (token1 == StreamTokenizer.TT_NUMBER) {
      cov.setAllowedValues(new OMCRangeList(parseRange(new Double(st.nval))));
    } else if (token1 == '{') {
      cov.setAllowedValues(parseSet());
    } else {
      throw unexpectedTokenException(7);
    }
    return cov;
  }

  private OMCRange parseRange(Comparable first) throws IOException {
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
          throw unexpectedTokenException(10);
        }
        if (isTo) {
          return new OMCToRange(first, last);
        } else {
          return new OMCThruRange(first, last);
        }
      }
    }
    pushBack();
    return new OMCPoint(first);
  }

  private OMCRangeList parseSet() throws IOException {
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
    return new OMCRangeList((OMCRange[]) values.toArray(new OMCRange[values.size()]));
  }

  private void pushBack() {
    pushedBack = true;
  }

  private int nextToken() throws IOException {
    int token;
    String caller;
    if (debug) caller = new Throwable().getStackTrace()[1].toString();
    if (pushedBack) {
      pushedBack = false;
      token = st.ttype;
      if (debug) System.out.println("nextToken from " + caller + ": repeat " + tokenAsString());
    } else {
      token = st.nextToken();
      if (debug) System.out.println("nextToken from " + caller + ": token " + tokenAsString());
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
        StringReader sbis = new StringReader(args[i]);
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
        FileReader sbis = new FileReader(args[i]);
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
