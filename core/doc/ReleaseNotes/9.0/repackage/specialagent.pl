#!/usr/bin/perl
# -*- Perl -*-

# <copyright>
#  Copyright 2001 BBNT Solutions, LLC
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

# deals with very special cases in the org.cougaar.core.agent package

# adds global imports
use Cwd;
use File::Copy;
use File::Path;
use File::Find;

$tmp = "addimports";
#local($dir) = $ARGV[0];
&main(@ARGV);

sub main {
    local(@args)=@_;
    local($dir)= $args[0];
    $found = 0;
    process_dir($dir);
    print "\n Found:", $found, " files to change.\n";
    print "done\n";
}

sub process_dir {
    local($dir)=@_;
    print("# Processing directory $dir");
    
    local(@files)=findfiles($dir);
    local($_);
    foreach $_ (@files) {
	$file = $_;
	#local($dev,$ino,$mode,$nlink,$uid,$gid) = lstat($_);
#	print "\nfile name is: ", $file;
	if (substr($file, -24) eq "AdvanceClockMessage.java") {
	    $found++;
	    process_alarm($_);
	} elsif (substr($file, -19) eq "ClusterContext.java") {
	    $found++;
	    process_service($_);
	} elsif (substr($file, -24)eq "ClusterServesPlugIn.java") {
	    $found++;
	    process_alarm($_);
	} elsif (substr($file, -16)eq "ClusterImpl.java") {
	    $found++;
	    add_services($_);
	} elsif (substr($file, -21)eq "MoveAgentMessage.java") {
	    $found++;
	    process_message($_);
	} elsif (substr($file, -17)eq "AgentManager.java") {
	    $found++;
	    process_message($_);
	}
    }
}

sub findfiles {
  local($dir) = @_;
  local(@stuff);
  local($code)= sub { 
      # skip cvs dirs
    if (/^CVS$/) {
      $File::Find::prune = 1;
      return;
    }
    #(($dev,$ino,$mode,$nlink,$uid,$gid) = lstat($_));
    push @stuff, $File::Find::name;
  };
  local(%ref);
  $ref{"wanted"}=$code;
  &find(\%ref, $dir);
  @stuff;
}

sub add_services {
    local($file) = @_;
    #print "\nprocessing file:", $file;
    open(IN, $file);
    open(OUT, ">".$tmp);
    while (<IN>) {
	$input_line = $_;
	printf (OUT $input_line);
	# find the package line and put the import lines after it.
	if (substr($input_line,0,7) eq "package") {
	    $found++;
	    printf OUT ("\nimport org.cougaar.core.service.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.blackboard.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.alarm.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.democontrol.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.domain.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.registry.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.scheduler.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.sharedthreading.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.uid.\*;\n");

	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}


sub process_alarm {
    local($file) = @_;
    #print "\nprocessing file:", $file;
    open(IN, $file);
    open(OUT, ">".$tmp);
    while (<IN>) {
	$input_line = $_;
	printf (OUT $input_line);
	# find the package line and put the import lines after it.
	if (substr($input_line,0,7) eq "package") {
	    $found++;
	    printf OUT ("\nimport org.cougaar.core.agent.service.alarm.\*;\n");
	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}

sub process_service {
    local($file) = @_;
    #print "\nprocessing file:", $file;
    open(IN, $file);
    open(OUT, ">".$tmp);
    while (<IN>) {
	$input_line = $_;
	printf (OUT $input_line);
	# find the package line and put the import lines after it.
	if (substr($input_line,0,7) eq "package") {
	    $found++;
	    printf OUT ("\nimport org.cougaar.core.service.\*;\n");
	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}

sub process_message {
    local($file) = @_;
    #print "\nprocessing file:", $file;
    open(IN, $file);
    open(OUT, ">".$tmp);
    while (<IN>) {
	$input_line = $_;
	printf (OUT $input_line);
	# find the package line and put the import lines after it.
	if (substr($input_line,0,7) eq "package") {
	    $found++;
	    printf OUT ("\nimport org.cougaar.core.mts.Message\;\n");
	    printf OUT ("\nimport org.cougaar.core.mts.MessageAddress\;\n");
	    printf OUT ("\nimport org.cougaar.core.node.NodeIdentifier\;\n");
	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}






