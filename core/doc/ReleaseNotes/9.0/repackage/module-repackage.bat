@echo OFF

REM "<copyright>"
REM " Copyright 2001 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

REM Run this to convert files to be compatible with Cougaar9.0
REM Usage module-repackage.bat <originalsrc> <repackaged_destination>
REM Example: module-repackage.bat C:\mystuff C:\mynewstuff

set ORIG_SRC=%1
set REPKG_DEST=%2

@ECHO ON

CALL core-rpkg.pl -m -d %REPKG_DEST% %ORIG_SRC% undomain.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% cougaar9core.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% postprocess.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% unclustersociety.pkg

CALL grossimports.pl %REPKG_DEST%

