set ORIG_SRC=%1
set REPKG_DEST=%2

@ECHO ON

CALL core-rpkg.pl -m -d %REPKG_DEST% %ORIG_SRC% undomain.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% cougaar9core.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% postprocess.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% unclustersociety.pkg

CALL specialglmnaming.pl %REPKG_DEST%\org\cougaar\mlm\ui\psp\naming

CALL grossimports.pl %REPKG_DEST%


