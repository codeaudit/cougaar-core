#!/usr/bin/perl
# -*- Perl -*-
# repackage  A repackager for java systems.
# usage:
# repackage [-cmvp] dir pkgdef+
#  -m  actually move files which match package prefixes.
#  -v  be verbose.  Print every command executed an report on
#      any files changed.
#  -p  dont actually do anything - just pretend.
#

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


# stuff for internal find
use Cwd;
use File::Copy;
use File::Path;
use File::Find;

require "getopts.pl";
&Getopts("cmvphd:");
&usage() if $opt_h;
$cvsP = $opt_c;
$moveP = $opt_m;
$verboseP = $opt_v;
$pretendP = $opt_p;
$realP = ! $pretendP;
$prefix = $opt_d;

$logP = $realP;

&usage() if ($#ARGV < 1);

# init globals
@abbr_old;
@abbr_new;
@pkg_old;
@pkg_new;
@dir_old;
@dir_new;

$stat_files_modified = 0;
$stat_files_moved = 0;

$tmp = "/tmp/core-repackage.$$";

&main(@ARGV);

sub lprint {
    local($rpt) = @_;
    printf STDERR "$rpt\n" if $verboseP;
    printf LOG "$rpt\n" if $logP;
}

sub parse_pkgdef {
    local($pn) = @_;
    
    open IN, "<$pn" || die("Couldn't open $pn");

    local($c) = 0;

    while (<IN>) {
	chop;			# drop newline
	s/\#.*//;		# trim comments
	if (/\W*([\w.]+)\W*=\W*([\w.]+)\W*/) {
	  push @abbr_old, $1;
	  push @abbr_new, $2;
	  #print "\nin equals if... values are: ", $1, " and ", $2;
	} elsif (/\W*([\w.]+[;])\W+([\w.]+[;])\W*/) {
	    $c++;		# inc patterncount;
	    $old = $1;
	    $new = $2;
	    $old = &convert_abbrev($old);
	    $new = &convert_abbrev($new);
	    push @pkg_old, quotemeta $old;
	    push @pkg_new, $new;
	} elsif (/\W*([\w.]+)\W+([\w.]+)\W*/) {
	    $c++;		# inc patterncount;
	    $old = $1;
	    $new = $2;
	    $old = &convert_abbrev($old);
	    $new = &convert_abbrev($new);
	    push @pkg_old, quotemeta $old;
	    push @pkg_new, $new;
	    $old =~ s/\./\//g;
	    $new =~ s/\./\//g;
	    push @file_old, $old;
	    push @file_new, $new;
	}
    }
    lprint("# Parsed $pn: $c rules");
    close IN;
}

sub xconvert_abbrev {
    local($_) = @_;
    study $_;
    # is there a better way to do this?
    s/^oc\./org\.cougaar\./;
    s/^occ\./org\.cougaar\.core\./;
    s/^ocd\./org\.cougaar\.domain\./;
    s/^ocdp\./org\.cougaar\.domain\.planning\./;
   
    $_;
}

sub convert_abbrev {
    local ($_) = @_;
    study $_;
    local($i);
    local($l)=$#abbr_old;
    for ($i=0;$i<=$l;$i++) {
	local($old)=quotemeta $abbr_old[$i];
	local($new)=$abbr_new[$i];
	s/^$old/$new/g;
    }
    return $_;
}


# package convert a whole file, using a temp file
sub process_file {
    local($file) = @_;

    $changed=0;
    if (isBinary($file)) {
	lprint("# not changing binary file $file");
    } else {
	open(IN, "<$file");
	open(OUT, ">$tmp") if ($realP);
	while (<IN>) {
	    # cleanup crlfs
	  $changed++ if (s/\r\n/\n/g);
	  $changed++ if (s/\r/\n/g);
	    $_ = &convert_pkg($_);
	    print OUT "$_" if ($realP);
	}
	close IN;
	close OUT if ($realP);
    }

    lprint("# modified $file ($changed changes)") if ($changed);

    local ($newname);
    if ($moveP) {
      $newname = &guess_rename($file);
    } else {
      $newname = $file;
    }
    
    $newname = "$prefix/$newname" if ($prefix);
    local ($renamed) = !($file eq $newname);

    lprint("# Warning: NOT moving $file") if (!$renamed && $moveP);

    if ($changed || $renamed) {
	$stat_files_modified++ if $changed;
	if ($renamed) {
	    $stat_files_moved++;
	    lprint("mv $file $newname");
	    check_target_dir($newname);
	    copyfile($tmp, $newname) if ($realP);
	    #unlink($file);  #assume we're moving elsewhere
	} else {
	    copyfile($tmp, $file) if ($realP);
	}
	unlink($tmp) if ($realP);
    }
    return $changed;
}

%checked_dirs;
sub check_target_dir {
    local($path) = @_;
    local(@v)=split('/',$path);
    local($l)=$#v;
    local($i);
    # don't check the final component, since that is the filename
    for ($i=0;$i<$l;$i++) {
	local($d) = join '/', @v[0..$i];
	if (! $checked_dirs{$d}) {
	    $checked_dirs{$d}=1;
	    careful_mkdir($d) if (! -d $d);
	}		
    }
}

# return true when argument names a binary file
sub isBinary {
    local($_) = @_;
    /\.(gif|jpg|png|zip|jar)$/;
}

# converts the packages of a string
sub convert_pkg {
    local ($_) = @_;
    study $_;
    local($i);
    local($l)=$#pkg_old;
    for ($i=0;$i<=$l;$i++) {
	local($old)=$pkg_old[$i];
	local($new)=$pkg_new[$i];
	if ( s/\b$old/$new/g ) {
	    # global used to detect if there were any changes made
	    $changed++;
	}
    }
    return $_;
}

sub guess_rename {
    local ($_) = @_;
    local($bak)=$_;
    local ($base)="";
    if (/([^\/]*\/)(.*)/) {
      $base=$1;
      $_=$2;
    }
    # study $_; # don't bother
    local($i);
    local($l)=$#pkg_old;
    for ($i=0;$i<=$l;$i++) {
	local($old)=$file_old[$i];
	local($new)=$file_new[$i];
	if ( s/^$old/$new/g ) {
	  return "$_";		# bail at the first match
	}
    }
    # no match, so return $_
    #return $bak;
    return $_;
}    

sub copyfile {
    local($old, $new) = @_;
    unlink($new);
    open(IN, "<$old");
    open(OUT, ">$new");
    while(<IN>)  {
      print OUT "$_";
    }
    close(OUT);
    close(IN);
}

sub report_stats {
    lprint("# Files modified: $stat_files_modified\n");
    lprint("# Files moved: $stat_files_moved\n");
}

sub usage {
   print STDERR "Usage: repackage [-mvph] [-d dest] dir pkgdef+
\t-m  actually move files which match package prefixes.
\t-v  be verbose.  Print every command executed an report on
\t     any files changed.
\t-p  dont actually do anything - just pretend.
\t-h  print this message and exit.
\t-d dest  copy files to new directory dest.
";
    exit 1;
}

sub process_dir {
    local($dir)=@_;
    lprint("# Processing directory $dir");
    
    local(@files)=findfiles($dir);
    local($_);
    foreach $_ (@files) {
	local($dev,$ino,$mode,$nlink,$uid,$gid) = lstat($_);
	if (-d $_) {
	    # consider creating new target directories up front
	    # let the files decide for now.
	    #consider_directory($_);
	} else {
	    process_file($_);
	}
    }
}

%madedir;
sub careful_mkdir {
    local($_) = @_;
    return if ($_ eq "");
    (($dev,$ino,$mode,$nlink,$uid,$gid) = lstat($_));
    if (-d $_) {
	# directory already there
	return;
    } else {
	if ($pretendP) {
	    # if we're pretending, make sure to record
	    # the mkdir so that we don't do it over and over...
	    return if $madedir{$_};
	    $madedir{$_}=1;
	}
	lprint("mkdir $_");
	mkdir($_, 777) if $realP;
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
    (($dev,$ino,$mode,$nlink,$uid,$gid) = lstat($_));
    push @stuff, $File::Find::name;
  };
  local(%ref);
  $ref{"wanted"}=$code;
  &find(\%ref, $dir);
  @stuff;
}


sub main {
    local(@args)=@_;
    local($dir)= $args[0];
    @args = @args[1,];
    

    open(LOG, ">/tmp/repackage.log") if $logP;

    # grok the packagedefs
    foreach $pkgd (@args) {
	parse_pkgdef($pkgd);
    }

    process_dir($dir);

    report_stats();

    lprint("# done");

    close(LOG) if $logP;

    exit 0;
}




