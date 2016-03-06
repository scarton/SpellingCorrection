package com.aitheras.spelling;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Creates a serialized Map from the "big.txt" file that represents a model of
 * unique words and counts.
 * 
 * The "main" method here can be used to build the model from the specified file
 * on the command line. The first arg will be that source text file. The second
 * (optional) argument is the file name of the model to be created - defaults to
 * spelling.model
 * 
 * Probablistic approach based on Norvic: http://norvig.com/spell-correct.html
 * 
 * @author Steve Carton (stephen.carton@aitheras.com) Jan 6, 2016
 *
 */
class SpellingModelBuilder {
	private String MODEL_NAME = "spelling.model";

	/**
	 * Reads the file, tokenizes and builds Hashmap
	 * 
	 * @param file
	 * @throws IOException
	 */
	public Map<String, Integer> loadBigText(File file) throws IOException {
		Map<String, Integer> nWords = new HashMap<String, Integer>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("\\w+");
		for (String temp = ""; temp != null; temp = in.readLine()) {
			Matcher m = p.matcher(temp.toLowerCase());
			while (m.find())
				nWords.put((temp = m.group()), nWords.containsKey(temp) ? nWords.get(temp) + 1 : 1);
		}
		in.close();
		return nWords;
	}

	/**
	 * Creates a simple field-value pair serialization from the HashMap.
	 * Serializes that to a binary DataStream Zips the result and writes to the
	 * specified path.
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void exportModel(Map<String, Integer> nWords, File path) throws IOException {
		FileOutputStream fos = new FileOutputStream(path.getPath() + "/" + MODEL_NAME + ".gz");
		GZIPOutputStream zos = new GZIPOutputStream(fos);
		DataOutputStream dos = new DataOutputStream(zos);
		dos.writeInt(nWords.size());
		for (Entry<String, Integer> entry : nWords.entrySet()) {
			dos.writeUTF(entry.getKey());
			dos.writeInt(entry.getValue());
		}
		dos.close();
	}

	/**
	 * Command line to build a binary spelling model from a large collection of
	 * properly spelled texts. SpellingModelBuilder largeTextFile.txt <
	 * <optional_model_name>> If the model name isn't specified, creates
	 * "spelling.model" which is a binary file serialized to spelling.model.gz
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		if (args.length > 0) {
			File bigTxt = new File(args[0]);
			File path = bigTxt.getParentFile();
			SpellingModelBuilder modelBuilder = new SpellingModelBuilder();
			if (args.length == 2) {
				modelBuilder.setMODEL_NAME(args[1]);
			}
			Map<String, Integer> nWords = modelBuilder.loadBigText(bigTxt);
			modelBuilder.exportModel(nWords, path);
		} else {
			System.err.println("Name of a source text file is required as the first argument.");
		}
	}

	public void setMODEL_NAME(String m) {
		MODEL_NAME = m;
	}
}
