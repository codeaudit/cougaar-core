#!/usr/bin/perl
# create a javaio.patch file based on the current sources
# for instance:
#   bin/makepatch.pl /usr/local/java/j2sdk1.4.1_02 /tmp/cougaar/latest/src/javaiopatch/src
#
$jdir = $ARGV[0];		# arg 0 is JAVAHOME
$ndir = $ARGV[1];		# arg 1 is javaiopatch/src

$tmpdir = "/tmp/mkjiopatch$$";
mkdir $tmpdir;

# step1 - copy the current sources
$dnew = "$tmpdir/new";
mkdir $dnew;
chdir $ndir;
system "tar cf - `find . -name \"*.java\" -print` | (cd $dnew; tar xf -)";

# step 2 - copy the old sources
$dold = "$tmpdir/old";
mkdir $dold;
chdir $dold;
system "unzip -o $jdir/src.zip > /dev/null";

# step 3 - compute the diff
chdir $tmpdir;
system "diff -bBudrw old new | grep -v 'Only in old' > $ndir/javaio.patch";


# done
chdir "/tmp";
system "rm -rf $tmpdir";
