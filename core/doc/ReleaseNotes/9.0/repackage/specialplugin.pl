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

# deals with very special cases in the org.cougaar.core.plugin package

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
	if (substr($file, -15) eq "LDMService.java") {
	    $found++;
	    process_domain($_);
	} elsif (substr($file, -23) eq "LDMServiceProvider.java") {
	    $found++;
	    process_domain($_);
	} elsif (substr($file, -11) eq "PlugIn.java") {
	    $found++;
	    process_domain($_);
	} elsif (substr($file, -18) eq "PlugInAdapter.java") {
	    $found++;
	    process_domainandservice($_);
	} elsif (substr($file, -18) eq "PlugInContext.java") {
	    $found++;
	    process_domain($_);
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

sub process_domain {
    local($file) = @_;
    #print "\nprocessing file:", $file;
    open(IN, $file);
    open(OUT, ">".$tmp);
    while (<IN>) {
	$input_line = $_;
	if (substr($input_line,0,34) eq "import org.cougaar.planning.ldm.\*;") {
	    $input_line = "import org.cougaar.core.domain.\*;\n";
	}
	printf (OUT $input_line);
	# find the package line and put the import lines after it.
	if (substr($input_line,0,7) eq "package") {
	    $found++;
	    printf OUT ("\nimport org.cougaar.core.domain.\*;\n");
	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}

sub process_domainandservice {
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
	    printf OUT ("\nimport org.cougaar.core.domain.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.service.\*;\n");
	    printf OUT ("\nimport org.cougaar.core.agent.service.alarm.\*;\n");
	}

    }
    close(OUT);
    close(IN);
    rename($tmp, $file);
}

