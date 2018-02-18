package fr.lip6.reden.nelinker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This class implements methods to create Lucene indexes and to search for names.
 * 
 * @author @author Brando & Frontini
 */
public class DicoProcessingNEL {

	private static Logger logger = Logger.getLogger(DicoProcessingNEL.class);
	
	/**
	 * It retrieves URIs from the dictionary for each mention.
	 * @param dirDico, folder of the dictionary
	 * @param nameDictionary, the name of the dictionary
	 * @param mentions, the mentions of the current paragraph
	 * @return the list of possible sets of URIs for each mention
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Map<String, List<List<String>>> retrieveMentionsURIsFromDico(
			String dirDico, String nameDictionary, List<String> mentions) {

		Date start = new Date();
		Map<String, List<List<String>>> out = new HashMap<String, List<List<String>>>();
		try {

			File folder = new File(dirDico + "/" + nameDictionary);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {

					CSVReader reader = new CSVReader(
							new InputStreamReader(new FileInputStream(dirDico
									+ "/" + nameDictionary + "/"
									+ listOfFiles[i].getName()), "UTF-8"),
							'\t', CSVWriter.NO_QUOTE_CHARACTER);
					String[] line;
					while ((line = reader.readNext()) != null) {

						for (String mention : mentions) {
							if (DicoProcessingNEL.replaceNonAlphabeticCharacters(mention)
									.equalsIgnoreCase(
											DicoProcessingNEL.replaceNonAlphabeticCharacters(line[0]))) {
								List<String> l = new ArrayList<String>();
								for (int k = 2; k < line.length; k++) {
									l.add(line[k]);
								}
								if (out.get(line[0]) == null) {
									List<List<String>> lc = new ArrayList<List<String>>();
									lc.add(l);
									out.put(line[0], lc);
								} else {
									List<List<String>> lc = out.get(line[0]);
									// avoiding to add an identical list
									boolean in = false;
									for (List<String> list : lc) {
										List listOne = Arrays.asList(list);
										List listTwo = Arrays.asList(l);
										if (listOne.equals(listTwo))
											in = true;
									}
									if (!in) {
										lc.add(l);
									}
								}
							}
						}
					}
					reader.close();
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Date end = new Date();
		logger.info("Finished retrieveMentionsURIsFromDico in "
				+ (end.getTime() - start.getTime()) / 60 + "secs");

		return out;
	}

	/**
	 * It retrieves URIs from the dictionary for each mention using the Lucene index.
	 * @param nameDictionary, the name of the dictionary
	 * @param mentions, the mentions of the current paragraph
	 * @param indexDirStr, folder of the dictionary index
	 * @return the list of possible sets of URIs for each mention
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Map<String, List<List<String>>> retrieveMentionsURIsFromDicoWithIndex(
			String nameDictionary, List<String> mentions, String indexDirStr) {

		Date start = new Date();
		Map<String, List<List<String>>> out = new HashMap<String, List<List<String>>>();
		for (String mention : mentions) {
			Set<String> results = DicoProcessingNEL.searchIndex(indexDirStr, "nameForm",
					DicoProcessingNEL.replaceNonAlphabeticCharacters(mention));
			for (String result : results) {
				String[] uris = result.trim().split("\t");
				List<String> l = new ArrayList<String>();
				for (int k = 0; k < uris.length; k++) {
					l.add(uris[k]);
				}
				if (out.get(mention) == null) {
					List<List<String>> lc = new ArrayList<List<String>>();
					lc.add(l);
					out.put(mention, lc);
				} else {
					List<List<String>> lc = out.get(mention);
					// avoiding to add an identical list
					boolean in = false;
					for (List<String> list : lc) {
						List listOne = Arrays.asList(list);
						List listTwo = Arrays.asList(l);
						if (listOne.equals(listTwo))
							in = true;
					}
					if (!in) {
						lc.add(l);
					}
				}
			}

		}
		Date end = new Date();
		logger.info("Finished retrieveMentionsURIsFromDicoWithIndex in "
				+ (end.getTime() - start.getTime()) / 60 + "secs");
		return out;
	}
	
	/**
	 * Main method to build index on dictionary files. 
	 * Similar to: https://github.com/need4spd/aboutLucene/blob/master/lucene-3.x/src/main/java/com/tistory/devyongsik/demo/IndexFiles.java
	 * 
	 * @param indexDirStr, index folder
	 * @param dataDirStr, dictionary data
	 */
	public static void createIndex(String indexDirStr, String dataDirStr, String indexField) {

		final Path docDir = Paths.get(dataDirStr);
		if (!Files.isReadable(docDir)) {
			System.out
					.println("Document directory '"
							+ docDir.toAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		logger.info("Data dir: " + docDir);
		Date start = new Date();
		try {
			logger.info("Indexing to directory '" + indexDirStr + "'...");
			Directory dir = FSDirectory.open(Paths.get(indexDirStr));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			iwc.setOpenMode(OpenMode.CREATE);
			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			// iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir, indexField);

			writer.close();
			Date end = new Date();
			logger.info(end.getTime() - start.getTime()
					+ " total milliseconds");
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes a set of documents.
	 * Similar to: https://github.com/need4spd/aboutLucene/blob/master/lucene-3.x/src/main/java/com/tistory/devyongsik/demo/IndexFiles.java
	 * @param writer
	 * @param path
	 * @throws IOException
	 */
	static void indexDocs(final IndexWriter writer, Path path, final String indexField)
			throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					try {
						if (!file.toFile().isHidden()) {
							indexDoc(writer, file, attrs.lastModifiedTime().toMillis(), indexField);
						}
					} catch (IOException ignore) {
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis(), indexField);
		}
	}

	/**
	 * Indexes a single document.
	 * Similar to: https://github.com/need4spd/aboutLucene/blob/master/lucene-3.x/src/main/java/com/tistory/devyongsik/demo/IndexFiles.java
	 * @param writer
	 * @param file
	 * @param lastModified
	 * @throws IOException
	 */
	static void indexDoc(IndexWriter writer, Path file, long lastModified, String indexField)
			throws IOException {
		try {

			InputStream stream = Files.newInputStream(file);
			CSVReader reader = new CSVReader(new InputStreamReader(
					new FileInputStream(file.toFile()), "UTF-8"), '\t',
					CSVWriter.NO_QUOTE_CHARACTER);
			String[] line = {};

			while ((line = reader.readNext()) != null) {
				// make a new, empty document
				Document doc = new Document();
				Field pathField = new StringField(indexField,
						replaceNonAlphabeticCharacters(line[0]),
						Field.Store.YES);
				doc.add(pathField);
				String uris = "";
				for (int k = 2; k < line.length; k++) {
					uris += "\t" + line[k];
				}
				doc.add(new StringField("uris", uris.trim(), Field.Store.YES));
				writer.addDocument(doc);
			}
			logger.info("file processed " + file);
			reader.close();
		} catch (Exception e) {
			System.err.println("error building index");
		}
	}

	/**
	 * Method to search for a phrase in the index built from the dictionary.
	 * @param index, name of the index
	 * @param field, name of the field to search within the index
	 * @param queryString, the phrase to search for
	 * @return the results
	 */
	public static Set<String> searchIndex(String index, String field,
			String queryString) {
		Set<String> results = new HashSet<String>();
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
					.get(index)));

			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new KeywordAnalyzer();
			QueryParser parser = new QueryParser(field, analyzer);
			parser.setDefaultOperator(QueryParser.Operator.AND);
			Query query = parser.parse(queryString.replaceAll(" ", "\\\\"));
			//System.out.println("Searching for: " + query.toString());
			// Date start = new Date();
			TopDocs hits = searcher.search(query, 100);
			// Date end = new Date();
			// System.out.println("Time: " + (end.getTime() - start.getTime())
			// + "ms");
			ScoreDoc[] scoreDocs = hits.scoreDocs;
			for (int n = 0; n < scoreDocs.length; ++n) {
				ScoreDoc sd = scoreDocs[n];
				int docId = sd.doc;
				Document d = searcher.doc(docId);
				results.add(d.get("uris"));
				//System.out.println("Name: " + d.get("nameForm"));
				//System.out.println("URIs found: " + d.get("uris"));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * Method to search for a phrase in the index built from the dictionary using regexp.
	 * This is used for computing population completeness
	 * @param index, name of the index
	 * @param field, name of the field to search within the index
	 * @param queryString, the phrase to search for
	 * @return the results
	 */
	public static Set<String> searchIndexWithRegexp(String index, String field,
			String queryString) {
		Set<String> results = new HashSet<String>();
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
					.get(index)));

			IndexSearcher searcher = new IndexSearcher(reader);
			RegexpQuery query = new RegexpQuery(new Term(field, queryString));
			
			// Date start = new Date();
			TopDocs hits = searcher.search(query, 20000000);
			// Date end = new Date();
			// System.out.println("Time: " + (end.getTime() - start.getTime())
			// + "ms");
			ScoreDoc[] scoreDocs = hits.scoreDocs;
			for (int n = 0; n < scoreDocs.length; ++n) {
				ScoreDoc sd = scoreDocs[n];
				int docId = sd.doc;
				Document d = searcher.doc(docId);
				results.add(d.get(field).split("\\t")[0].replaceAll("#foaf:Person", "")); //particular fix for BNF, actually I should fix the Sparql query to bnf to get proper URIs
				//System.out.println(d.get(field).split("\\t")[0]);
				//System.out.println("URIs found: " + d.get("uris"));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return results;
	}
	
	/**
	 * Build an inverted index of URIs for obtaining the corresponding mention.
	 * 
	 * @param mentionsWithURIs
	 *            , the mentions and their URIs for every possible candidate
	 * @return the new index
	 */
	public static Map<String, String> buildInvertedIndex(
			Map<String, List<List<String>>> mentionsWithURIs) {
		Map<String, String> index = new HashMap<String, String>();
		for (String mention : mentionsWithURIs.keySet()) {
			List<List<String>> listsOfURIsForMention = mentionsWithURIs
					.get(mention);
			for (List<String> listOfUris : listsOfURIsForMention) {
				for (String uri : listOfUris) {
					index.put(uri, mention);
				}
			}
		}
		logger.info("Finished buildInvertedIndex");
		return index;
	}
	
	/**
	 * Remove special characters and spaces in mentions if necessary.
	 * 
	 * @param in
	 *            , the string
	 * @return the new string
	 */
	public static String replaceNonAlphabeticCharacters(String in) {
		Pattern p = Pattern.compile("\\s|'|-");
		Matcher m = p.matcher(in);
		String texteRemplace = m.replaceAll("");
		return texteRemplace.toLowerCase();
	}
}
