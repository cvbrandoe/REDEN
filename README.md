# REDEN
Unsupervised graph-based tool for disambiguation and linking of named entities to Linked Data sets for digital editions in XML-TEI, it includes a Linked Data data extractor for building the dictionary of potential NE candidates

The main Java class to launch REDEN is fr.lip6.reden.MainNELApp.java, REDEN configuration file is located in config/config.properties, it should be properly configured before execution. There are two modes to launch REDEN 

The first mode allows the downloading and the constitution of a dictionary of potential NE candidates from Linked Data, so you must be connected to the Internet (when passing through a proxy use the following JVM parameters: -DproxySet=true -DproxyHost=IP_address -DproxyPort=port). So far, the place and the person (author) dictionaries are created from French DBpedia and the French National Library (BnF) Linked Data sets, respectively. Please contact us for helping you add a new LD source.

The only parameters are:  

java -cp "target/REDEN.jar:target/dependency/*" fr.lip6.reden.MainNELApp config/config-file.properties -createDico=bnf|dbpediafr|getty-per|all

In the properties file, the most important parameter to set is outDictionnaireDir which corresponds to the folder where REDEN will store the new dictionary files.

Once the dictionary has been built, the second mode allows the annotation of an input TEI-XML file, only the first time you launch REDEN you must be connected to the Internet. The parameters are:

java -cp "target/REDEN.jar:target/dependency/*" fr.lip6.reden.MainNELApp config-file.properties TEI-fileName.xml -printEval -createIndex -relsFile\=file -outDir\=dir

where:

config-file.properties (mandatory): the name of the properties file containing the parameters for configuring REDEN and the LD extractor, see config/config.properties

TEI-fileName.xml (mandatory): the name of the TEI file, include the file path if necessary

-printEval (optional): if an already annotated TEI file is available (i.e. a gold standard), REDEN will compare this file with the resulting annotated file and will provide accuracy measures, the name of gold file must match the name of the input file and end by "-gold.xml", for instance, of the input file is "apollinaire_heresiarque-et-cie.xml", the gold file should be named "apollinaire_heresiarque-et-cie-gold.xml" 

-createIndex (optional): REDEN creates Lucene indexes for improving access to the dictionary files. When executing REDEN the first time or when the dictionary has changed, it is mandatory to launch it using this flag, otherwise you can leave it out

-relsFile\=file (optional): file name listing the RDF predicates and their corresponding weights 

-outDir\=dir (optional): name of the folder where REDEN will output files: the annotated XML-TEI and other files which provide execution information

For instance, the following command will annotate the thibaudet TEI file using the data.BNF linked data repository:

java -cp "target/REDEN.jar:target/dependency/*" fr.lip6.reden.MainNELApp config/config-authors-bnf.properties input/thibaudet_reflexions.xml -printEval -outDir=output/

Besides, REDEN can extract information from a Linked Data sets thanks to URIs encoded within the TEI then it outputs a JSON data which can loaded in an specific purpose Web-based application in order to visualize these data in several ways, so far Web maps and gallery of author pics. You can find below the program argument to do so, please note that the TEI should contain already URIs for every named-entity in the ref attribute of the persName tag (or any other configurable in the properties file) :

config-file.properties tei-fileName-withURIs.xml -produceData4Visu=output.json -propsFile=<config_ld_properties>

-produceData4Visu=output.json (optional): name of the output JSON file 

-propsFile=<config_ld_properties> (optional): name of the properties file containing the name of the properties concerned by the extraction, for an example, see the files config/latlong.properties or config/authors.properties

In the config-file.properties, the parameter addScores must be set to false.
 
If you clone this repository and update the source code, then you may need to regenerate the JAR file, for that, you just need to install Maven and run the command 'mvn package'. Do not forget to modify the pom.xml to add the path to your local JDK install.

###### How to cite this work

Brando, C., Frontini, F., Ganascia, J.G. (2016) REDEN: Named-Entity Linking in digital Literary Editions using Linked Data Sets, Complex Systems Informatics and Modeling Quarterly CSIMQ, Issue 7, June/July 2016, pp. 60-79, published online by RTU Press, https://csimq-journals.rtu.lv, http://dx.doi.org/10.7250/csimq.2016-7.04 ISSN: 2255-9922 online

Brando, C., Frontini, F., Ganascia, J.G. (2015): Disambiguation of named entities in cultural heritage texts using linked data sets. In: Proceedings of the First International Workshop on Semantic Web for Cultural Heritage in Conjunction with 19th East-European Conference on Advances in Databases and Information Systems, New Trends in Databases and Information Systems, Springer, 539, Poitiers, France, http://link.springer.com/chapter/10.1007%2F978-3-319-23201-0_51 

Frontini F, Brando C, Ganascia J-G, (2015) Semantic Web based Named Entity Linking for digital humanities and heritage texts, in Proceedings of the First International Workshop Semantic Web for Scientific Heritage at the 12th ESWC 2015 Conference, Portorož, Slovenia, June 1st, 2015, pp. 77-88, URL: http://ceur-ws.org/Vol-1364/paper9.pdf 

Frontini, F., Brando, C., Ganascia, J.G. (2015): Domain-adapted named-entity linker using linked data. In: Proceedings of the 1st Workshop on Natural Language Applications: completing the puzzle in conjunction with the 20th International Conference on Applications of Natural Language to Information Systems, Passau, Germany, June 17-19, http://ceur-ws.org/Vol-1386/named_entity.pdf 

Brando, C., Frontini, F., Ganascia, J.G. (2015). Linked data for toponym linking in French literary texts. In Proceedings of the 9th Workshop on Geographic Information Retrieval (GIR '15), Ross S. Purves and Christopher B. Jones (Eds.). ACM, New York, NY, USA, Article 3 , 2 pages. DOI=http://dx.doi.org/10.1145/2837689.2837699



REDEN uses the following frameworks: 
- indexes are implemented with Lucene (https://lucene.apache.org/core/)
- RDF data is processed with the Apache Jena API (https://jena.apache.org/) 
- graphs are manipulated by the JgraphT API (http://jgrapht.org)
- implementation of centrality measures are available in the Social Network analysis tool JgraphT-SNA (https://bitbucket.org/sorend/jgrapht-sna).
