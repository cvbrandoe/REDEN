# REDEN
Unsupervised graph-based tool for disambiguation and linking of named entities to Linked Data sets for digital editions in XML-TEI, it includes a Linked Data data extractor for building the dictionary of potential NE candidates

The main Java class to launch REDEN is fr.lip6.reden.MainNELApp.java, REDEN configuration file is located in config/config.properties, it should be properly configured before execution. There are two modes to launch REDEN 

The first mode allows the downloading and the constitution of a dictionary of potential NE candidates from Linked Data, so you must be connected to the Internet (when passing through a proxy use the following JVM parameters: -DproxySet=true -DproxyHost=IP_address -DproxyPort=port). So far, the place and the person (author) dictionaries are created from French DBpedia and the French National Library (BnF) Linked Data sets. Please contact us for helping you add a new LD source.

The only parameter is:  

-createDico bnf|dbpediafr|all

Once the dictionary has been built, the second mode allows the annotation of an input TEI-XML file, only the first time you launch REDEN you must be connected to the Internet. The parameters are:

TEI-fileName.xml -printEval -createIndex -relsFile\=file -outDir\=dir

where:

TEI-fileName.xml (mandatory): the name of the TEI file, include the file path if necessary

-printEval (optional): if an already annotated TEI file is available (i.e. a gold standard), REDEN will compare this file with the resulting annotated file and will provide accuracy measures, the name of gold file must match the name of the input file and end by "-gold.xml", for instance, of the input file is "apollinaire_heresiarque-et-cie.xml", the gold file should be named "apollinaire_heresiarque-et-cie-gold.xml" 

-createIndex (optional): REDEN creates Lucene indexes for improving access to the dictionary files. When executing REDEN the first time or when the dictionary has changed, it is mandatory to launch it using this flag, otherwise you can leave it out

-relsFile\=file (optional): file name listing the RDF predicates and their corresponding weights 

-outDir\=dir (optional): name of the folder where REDEN will output files: the annotated XML-TEI and other files which provide execution information

REDEN uses the following frameworks: 
- indexes are implemented with Lucene (https://lucene.apache.org/core/)
- RDF data is processed with the Apache Jena API (https://jena.apache.org/) 
- graphs are manipulated by the JgraphT API (http://jgrapht.org)
- implementation of centrality measures are available in the Social Network analysis tool JgraphT-SNA (https://bitbucket.org/sorend/jgrapht-sna).