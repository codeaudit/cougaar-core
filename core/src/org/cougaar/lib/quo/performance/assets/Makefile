CLASSPATH=${COUGAAR_INSTALL_PATH}/lib/core.jar:${COUGAAR_INSTALL_PATH}/lib/build.jar
export CLASSPATH

build: programmer_assets.gen properties.gen

programmer_assets.gen: programmer_assets.def
	java org.cougaar.tools.build.AssetWriter  \
		-Porg.cougaar.lib.quo.performance.assets programmer_assets.def

properties.gen: properties.def
	java org.cougaar.tools.build.PGWriter properties.def


clean:
	rm -f *.java *.index *.gen *.class
