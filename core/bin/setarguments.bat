@echo OFF

rem Domains are now usually defined by the config file LDMDomains.ini
rem But you may still use properties if you wish.
rem set MYDOMAINS=-Dalp.domain.alp=org.cougaar.domain.glm.ALPDomain
set MYDOMAINS=
set MYCLASSES=org.cougaar.core.society.Node
set MYPROPERTIES=%MYDOMAINS% -Dalp.system.path=%ALP3RDPARTY% -Dalp.install.path=%ALP_INSTALL_PATH% -Duser.timezone=GMT
set MYMEMORY=-Xms100m -Xmx300m

