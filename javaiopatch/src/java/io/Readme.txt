Diffs have been moved to a standard (unified diff) patch file in the
base src directory of the module.

There are now two scripts in javaiopatch/bin,  makepatch generates a
patch file from sources and applypatch applies such a patch file to
the standard java sources and leaves the result in the right place
(suitable for checking in).

If you modify the sources in this directory, make certain to
regenerate the patchfile and track the appropriate jdk version
numbers.

Example 1:  I converted the base sources from 1.4.1_02 to 1.4.2_03
with the following sequence of commands:
% cd /tmp/cougaar/latest/src/javaiopatch
% bin/makepatch.pl /usr/local/java/j2sdk1.4.1_02 /tmp/cougaar/latest/src/javaiopatch/src
% bin/applypatch.pl /usr/local/java/j2sdk1.4.2_03 /tmp/cougaar/latest/src/javaiopatch/src

Example 2:  To regenerate the patchfile from the current sources:
% cd /tmp/cougaar/latest/src/javaiopatch
% bin/applypatch.pl /usr/local/java/j2sdk1.4.2_03 /tmp/cougaar/latest/src/javaiopatch/src
