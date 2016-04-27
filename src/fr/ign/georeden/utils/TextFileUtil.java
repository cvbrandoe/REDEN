package fr.ign.georeden.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TextFileUtil {
	private TextFileUtil() {
		
	}
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static String readFileUTF8(String path) throws IOException {
		return readFile(path, StandardCharsets.UTF_8);
	}

	public static List<String> readFileAllLines(String path, Charset encoding) throws IOException {
		return Files.readAllLines(Paths.get(path), encoding);
	}
}
