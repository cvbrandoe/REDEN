The DBpedia Spotlight project Jar is temporarily installed in the local repository as explained hereafter:

mvn install:install-file -Dfile=eclipseworkspace/nel/thirdPartyLibs/dbpedia-spotlight.jar 
-DgroupId=org.dbpedia -DartifactId=spotlight -Dversion=0.7 -Dpackaging=jar -DgeneratePom=true