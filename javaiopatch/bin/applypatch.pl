#!/usr/bin/perl
# apply a patch to a new set of java sources in order to shift java versions
# for instance:
#   bin/applypatch.pl /usr/local/java/j2sdk1.4.2_03 /tmp/cougaar/latest/src/javaiopatch/src
#
$jdir = $ARGV[0];		# arg 0 is JAVAHOME
$ndir = $ARGV[1];		# arg 1 is javaiopatch/src

$tmpdir = "/tmp/mkjiopatch$$";
mkdir $tmpdir;

# step 1 - grab the old sources
$dold = "$tmpdir/old";
mkdir $dold;
chdir $dold;
system "unzip -o $jdir/src.zip>/dev/null";

# step 2 - apply the patch (better be in the right spot
open FS, "patch -p1 < $ndir/javaio.patch|";
@fs;
while (<FS>) {
  if (/patching file (.*\java)/) {
    push @fs, $1;
  }
}
close FS;
foreach $f (@fs) {
  print "Updating $f\n";
  system("cp $f $ndir/$f");
}


# done
chdir "/tmp";
system "rm -rf $tmpdir";
