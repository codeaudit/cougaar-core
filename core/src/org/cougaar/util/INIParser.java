/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import org.cougaar.util.StringUtility;

/** Utility for reading MSWindows-style .ini files.
 **/

public class INIParser {

  private boolean isVerbose = false;
  public boolean isVerbose() { return isVerbose; }
  public void setVerbose(boolean v) { isVerbose = v; }

  private char commentChar = '#';
  private String filename = "<stream>";
  public char getCommentChar() { return commentChar; }
  public void setCommentChar(char c) { commentChar = c; }

  public static class SlotHolder {
    /** vector of Slot **/
    private Vector slots = new Vector();
    public Vector getSlots() { return slots; }
    public void addSlot(Slot s) { slots.addElement(s); }
    public Slot getSlot(String name) {
      int ix = slots.indexOf(name);
      if (ix < 0)
	return null;
      return (Slot) slots.elementAt(ix);
    }
  }

  public static class Group extends SlotHolder {
    private Vector sections = new Vector();

    public Vector getSections() { return sections; }
    public void addSection(Section s) { sections.addElement(s); }
  }

  public static class Section extends SlotHolder {
    /** name of Section **/
    private String name = null;
    public String getName() { return name; }
    
    /** Vector of String **/
    private Vector parameters = new Vector();
    public Vector getParameters() { return parameters; }
    public void addParameter(String p) { parameters.addElement(p); }

    public Section(String name) { this.name = name; }

    public String toString() { return "Section "+name; }
  }

  public static class Slot {
    private String name;
    public String getName() { return name; }
    
    private Vector values = new Vector();
    public Vector getValues() { return values; }
    public String getValue() {
      if (values.size() < 1) return null;
      return (String) values.elementAt(0);
    }
    public void setValues(Vector v) { values = v; }
    public void addValue(String v) { values.addElement(v); }

    public Slot(String name) { this.name = name; }
    public String toString() { return "Slot "+name; }
    public boolean equals(Object obj) {
      if (obj instanceof String) {
	return name.equals(obj);
      }
      if (obj instanceof Slot) {
	return name.equals(((Slot) obj).name);
      }
      return false;
    }
  }

  public Group parse(String filename) throws IOException {
    this.filename = filename;
    InputStream stream = null;
    
    if (filename.equals("-")) {
      if (isVerbose)
        System.err.println("Reading from standard input.");
      stream = new java.io.DataInputStream(System.in);
    } else {
      if (isVerbose)
        System.err.println("Reading \""+filename+"\".");
      stream = new FileInputStream(filename);
    }

    Group g = parse(stream);
    stream.close();
    return g;    
  }

  public Section parse(String filename, String section) throws IOException {
    this.filename = filename;
    InputStream stream = null;
    
    if (filename.equals("-")) {
      if (isVerbose)
        System.err.println("Reading from standard input.");
      stream = new java.io.DataInputStream(System.in);
    } else {
      if (isVerbose)
        System.err.println("Reading \""+filename+"\".");
      stream = new FileInputStream(filename);
    }

    Section s = parse(stream, section);
    stream.close();
    return s;    
  }


  public Section parse(InputStream in, String section) throws IOException {
    InputStreamReader isr = new InputStreamReader(in);
    BufferedReader br = new BufferedReader(isr);
    
    Group g = runParser(br);
    if (g != null) {
      Enumeration sections = g.getSections().elements();
      while (sections.hasMoreElements()) {
        Section s = (Section) sections.nextElement();
        if (section.equals(s.getName())) {
          return s;
        }
      }
    }

    return null;
  }

  public Group parse(InputStream in) throws IOException {
    InputStreamReader isr = new InputStreamReader(in);
    BufferedReader br = new BufferedReader(isr);
    return runParser(br);
  }

  private Group runParser(BufferedReader br) throws IOException {
    Group g = new Group();
    Section section = null;

    String line;
    int ln = 0;
    for (line = br.readLine(); line != null; line=br.readLine()) {
      int i, j;

      ln++;

        // ignore comments
      if ((i = line.indexOf(commentChar)) >= 0) 
        line = line.substring(0,i);

        // zap extra whitespace
      line = line.trim();

      // ignore empty lines
      if (line.length() <= 0)
        continue;

      int l;
      while (line.charAt((l=line.length())-1) == '\\') {
        line = line.substring(0,l-1) +
          br.readLine().trim();
        ln++;
      }

      if (line.charAt(0) == '[') {
        // classd line
        j = line.indexOf(']');
        String stuff = line.substring(1, j).trim();

        Vector v = StringUtility.explode(stuff);
          
        if (v.size() < 1) {
          throw new RuntimeException("Empty Section at line "+ln);
        }

        section = new Section((String) v.elementAt(0));
        for (i=1; i<v.size(); i++) {
          section.addParameter((String) v.elementAt(i));
        }

        g.addSection(section);
      } else if ( (j = line.indexOf('=')) >= 0) {
        // a param line

        String name = line.substring(0, j).trim();
        Vector v = StringUtility.parseCSV(line, j+1);
          
	Slot slot = new Slot(name);
	slot.setValues(v);
        if (section == null) {
          g.addSlot(slot);
        } else {
	  section.addSlot(slot);
	}
      } else {
        System.err.println("Bad line " + ln + " in " + filename);
      }
    }
    return g;
  }
}
