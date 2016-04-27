package fr.ign.georeden.utils;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {

	/**
	 * Gets the string with the key name from targeted file.
	 *
	 * @param key
	 *            the key
	 * @param filePath
	 *            the file path
	 * @return the string from file
	 * @throws JSONException
	 *             the JSON exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static String getStringFromFile(String key, String filePath) throws JSONException, IOException {
		JSONObject obj = new JSONObject(TextFileUtil.readFileUTF8(filePath));
		return obj.getString(key);// obj.getJSONArray("results")
	}
	
	public static boolean getBooleanFromFile(String key, String filePath) throws JSONException, IOException {
		JSONObject obj = new JSONObject(TextFileUtil.readFileUTF8(filePath));
		return obj.getBoolean(key);// obj.getJSONArray("results")
	}


	/**
	 * Gets the string array with the key name from targeted file.
	 *
	 * @param key
	 *            the key
	 * @param filePath
	 *            the file path
	 * @return the string array from file
	 * @throws JSONException
	 *             the JSON exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static String[] getStringArrayFromFile(String key, String filePath) throws JSONException, IOException {
		JSONObject obj = new JSONObject(TextFileUtil.readFileUTF8(filePath));
		JSONArray array = obj.getJSONArray(key);
		String[] results = new String[array.length()];
		for (int i = 0; i < results.length; i++) {
			results[i] = array.getString(i);
		}
		return results;
	}
}
