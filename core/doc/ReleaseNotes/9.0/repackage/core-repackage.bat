@ECHO ON

set ORIG_SRC=%1
set NODOMAIN=%2\..\nodomaincore
set PRE_DEST=%2\..\precore
set REPKG_DEST=%2

CALL core-rpkg.pl -m -d %NODOMAIN% %ORIG_SRC% undomain.pkg

CALL core-rpkg.pl -m -d %PRE_DEST% %NODOMAIN% cougaar9core.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST% %PRE_DEST% postprocess.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent %REPKG_DEST%\org\cougaar\core\agent agent.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\service %REPKG_DEST%\org\cougaar\core\service service.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\alarm %REPKG_DEST%\org\cougaar\core\agent\service\alarm alarm.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\democontrol %REPKG_DEST%\org\cougaar\core\agent\service\democontrol democontrol.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\scheduler %REPKG_DEST%\org\cougaar\core\agent\service\scheduler scheduler.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\sharedthreading %REPKG_DEST%\org\cougaar\core\agent\service\sharedthreading sharedthreading.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\uid %REPKG_DEST%\org\cougaar\core\agent\service\uid uid.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\blackboard %REPKG_DEST%\org\cougaar\core\blackboard blackboard.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\mts %REPKG_DEST%\org\cougaar\core\mts mts.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\util %REPKG_DEST%\org\cougaar\core\util util.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\node %REPKG_DEST%\org\cougaar\core\node node.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\registry %REPKG_DEST%\org\cougaar\core\agent\service\registry registry.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\agent\service\domain %REPKG_DEST%\org\cougaar\core\agent\service\domain domain.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\core\domain %REPKG_DEST%\org\cougaar\core\domain occdomain.pkg

CALL core-rpkg.pl -m -d %REPKG_DEST%\org\cougaar\planning\plugin %REPKG_DEST%\org\cougaar\planning\plugin coreplugin.pkg

REM get rid of any left over references to cluster and society
CALL core-rpkg.pl -m -d %REPKG_DEST% %REPKG_DEST% unclustersociety.pkg

REM add extra imports of each other for packages that got split into 2
REM ie cluster.* went to blackboard and agent
CALL extraimports.pl %REPKG_DEST%\org\cougaar\core\agent

CALL extraimports.pl %REPKG_DEST%\org\cougaar\core\blackboard

CALL extraimports.pl %REPKG_DEST%\org\cougaar\core\mts

CALL extraimports.pl %REPKG_DEST%\org\cougaar\core\node

REM allow service impls to import their service api's and agent*
CALL serviceimpl.pl %REPKG_DEST%\org\cougaar\core\agent\service
CALL myserviceapi.pl %REPKG_DEST%\org\cougaar\core\logging
CALL myserviceapi.pl %REPKG_DEST%\org\cougaar\core\mts
CALL myserviceapi.pl %REPKG_DEST%\org\cougaar\core\naming

REM special case add occ.domain.* to these service impls
CALL domainimpl.pl %REPKG_DEST%\org\cougaar\core\agent\service\domain
CALL domainimpl.pl %REPKG_DEST%\org\cougaar\core\agent\service\registry

REM special case add occ.agent.service.alarm.* to these service impls
CALL democontrolimpl.pl %REPKG_DEST%\org\cougaar\core\agent\service\democontrol

REM special blackboard adds to lps
CALL lps.pl %REPKG_DEST%\org\cougaar\planning\ldm\lps

REM more special cases
CALL specialagent.pl %REPKG_DEST%\org\cougaar\core\agent
CALL specialservice.pl %REPKG_DEST%\org\cougaar\core\service
CALL specialplan.pl %REPKG_DEST%\org\cougaar\planning\ldm\plan
CALL specialasset.pl %REPKG_DEST%\org\cougaar\planning\ldm\asset
CALL specialplugin.pl %REPKG_DEST%\org\cougaar\core\plugin
CALL specialblackboard.pl %REPKG_DEST%\org\cougaar\core\blackboard
CALL specialnode.pl %REPKG_DEST%\org\cougaar\core\node
CALL specialcoreutil.pl %REPKG_DEST%\org\cougaar\core\util
CALL specialpluginutil.pl %REPKG_DEST%\org\cougaar\core\plugin\util