set ORIG_SRC=%1
set REPKG_DEST=%2

@ECHO ON

CALL core-rpkg.pl -m -d %REPKG_DEST% %ORIG_SRC% undomain.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% cougaar9core.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% postprocess.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% unclustersociety.pkg

CALL specialglmasset.pl %REPKG_DEST\org\cougaar\glm\ldm\asset
CALL specialglmlps.pl %REPKG_DEST\org\cougaar\glm\ldm\lps
CALL specialglmparser.pl %REPKG_DEST\org\cougaar\glm\parser
CALL specialglminventory.pl %REPKG_DEST\org\cougaar\glm\plugins\inventory
CALL specialglminventory.pl %REPKG_DEST\org\cougaar\glm\plugins\multiplesuppliers
CALL specialmlmexamples.pl %REPKG_DEST\org\cougaar\mlm\examples
CALL specialmlmuilogplanview.pl %REPKG_DEST\org\cougaar\mlm\ui\logplanview
CALL specialglmpsp.pl %REPKG_DEST\org\cougaar\mlm\ui\perturbation\asset
CALL specialglmpsp.pl %REPKG_DEST\org\cougaar\mlm\ui\psp\plan
CALL specialglmpsp.pl %REPKG_DEST\org\cougaar\mlm\ui\psp\transportation
CALL specialglmnaming.pl %REPKG_DEST\org\cougaar\mlm\ui\psp\naming


