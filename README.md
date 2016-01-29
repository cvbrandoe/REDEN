# REDEN
Unsupervised graph-based tool for disambiguation and linking of named entities to Linked Data sets for digital editions in XML-TEI.

The main Java class to launch REDEN is fr.lip6.reden.MainNELApp.java, REDEN configuration file is located in config/config.properties, it should be properly configured before execution. The arguments are (only the first one is mandatory): 

<TEI-fileName.xml> -printEval -createIndex -relsFile=<file> -outDir=<dir>

where:

<TEI-fileName.xml>: the name of the TEI file indicating if necessary the file path

-printEval: if an already annotated TEI file is available (i.e. a gold standard), REDEN will compare this file with the resulting annotated file and will provide accuracy measures

-createIndex: REDEN creates Lucene indexes for improving access to the dictionary files. When executing REDEN the first time or when the dictionary has changed, it is mandatory to launch it using this flag, otherwise you can leave it out

-relsFile=<file>: RDF predicates file with an associated weight

-outDir=<dir>: name of the folder where REDEN will output files: the annotated XML-TEI and other files which provide execution information

REDEN uses the following frameworks: 
- indexes are implemented with Lucene (https://lucene.apache.org/core/)
- RDF data is processed with the Apache Jena API (https://jena.apache.org/) 
- graphs are manipulated by the JgraphT API (http://jgrapht.org)
- implementation of centrality measures are available in the Social Network analysis tool JgraphT-SNA (https://bitbucket.org/sorend/jgrapht-sna).

Java binaries will soon be available!