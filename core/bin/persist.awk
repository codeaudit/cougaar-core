# -*- Awk -*-

# <copyright>
#  Copyright 2002 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
# 
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
# 
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>

# Cougaar log4j log parser to extract persistence information.
#
# The log4j configuration must include these lines:
#   log4j.category.org.cougaar.core.blackboard.Distributor=INFO
#   log4j.category.org.cougaar.core.persist.BasePersistence=INFO
#
# Parse the log4j ".log" files by running:
#   awk -f persist.awk *.log > persist.csv
#
# View the output with Gantt Chart UI (bug 2415):
#   java \
#    -classpath /opt/cougaar/lib/core.jar:/opt/cougaar/lib/datagrabber.jar \
#    org.cougaar.mlm.ui.newtpfdd.gui.view.FileGanttChartView \
#    persist.csv
#
# The UI will show per-agent rows, where the color on for each
# row segment indicates:
#    grey:    normal running, periodically interrupted to persist, e.g.
#               every 5 minutes
#    blue:    waiting for transactions to close.  Mouse-over will show
#               the number of open transactions
#    yellow:  waiting for jvm persist lock (bug 2370)
#    red:     serializing while in the jvm persist lock.  This causes
#               other agents in the node to display yellow.
#    green:   writing the persistence shapshot to I/O
#
# This sed command will remove the "node__" prefix:
#    cat output.csv | \
#     sed -e 's/[^_]*__//' \
#     > output2.csv
#
# This sed command will reorder the output to obtain "agent__node" order:
#    cat output.csv | \
#     sed -e 's/\([^_]*\)__\([^,]*\)/\2__\1/' \
#     > output3.csv
#
# This awk script is nearly perl, so I should probably port it!

BEGIN {
  # we'll use "," as our comma-separated-value
  FS=",";
  print "# view with the Gantt Chart UI (see awk script for details)";
}
{
  # only care about persistence lines
  if (! /Distributor | BasePersistence/) next; 

  # drop timestamp millis & seconds digit (e.g. "12:34,567 -> "12:30")
  sub("[0-9],[0-9]*", "0"); 

  # drop the " INFO - class - "
  sub(" [^-]*-[^-]*- ", ","); 

  # replace the "Agent: " with "Agent, "
  sub(": ", ","); 

  # replace the short time with a full date
  # NOTE: this won't work in general!
  gsub("^0", "2002-10-9 0"); 

  # get the file name w/o the ".log" extension
  # for some reason we can't do this when we BEGIN
  file=FILENAME;
  sub("\.log", "__", file); 

  # okay examine the current line
  if (sub("Distributor started", "grey\, Distributor started")) {

    # first line for this agent, so we set the "prior[]" timestamp
    prior[$2]=$1;

    # print the line
    print file $2 ", " $1 ", " $1 ", " $3 ", " $4;

  } else if (\
      sub("Waiting", "blue\,Waiting")) { \

    # we really want "waited", to tell when the waiting finished.
    # we'll squirrel this away, pending the "Persist started (persist)"
    waiting[$2]=sprintf("%s, %s", $3, $4);

  } else if (\
      /Persist started \(persist\)/ ) {

    # okay, transaction lockout wait is over
    t=prior[$2];
    if ($1 != t) {
      # the wait crossed the 10 seconds boundary, so we print it.
      print file $2 ", " prior[$2] ", " $1 ", " waiting[$2];
      prior[$2]=$1;
    }
  } else if (\
      sub("Persist requested \\\(persist\)", "grey\,Running until timer persist") || \
      sub("Persist started \\\(finish transaction\)", "grey\,Running until timer transaction") || \
      sub("Obtained", "yellow\,Blocked by") || \
      sub("Serialized", "red\,Serialized") || \
      sub("Persisted", "green\,Writing")) { 
    # more mangling of the line, plus setting the color
    t=prior[$2];
    if ($1 != t) {
      print file $2 ", " prior[$2] ", " $1 ", " $3 ", " $4;
      prior[$2]=$1;
    }
  }
}
