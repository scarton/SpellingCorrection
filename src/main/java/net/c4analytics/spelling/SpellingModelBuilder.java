package net.c4analytics.spelling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates a serialized Map from the "big.txt" file that represents a model -
 * unique words and counts.
 * 
 * The "main" method here can be used to build the model from the specified file
 * on the command line. The first arg will be that source text file. The second
 * (optional) argument is the file name of the model to be created - defaults to
 * spelling.model
 * 
 * Probablistic approach based on Norvic: http://norvig.com/spell-correct.html
 * 
 * @author Steve Carton (stephencarton@gmail.com) Dec 22, 2015
 *
 */
public class SpellingModelBuilder {
	static {
		System.setProperty("Log.File", "SpellingModelBuilder");
	}
	final static Logger logger = LogManager.getLogger(SpellingModelBuilder.class.getName());
	private static final String MODEL_NAME = "spelling.model";

	/**
	 * Reads the file, tokenizes and builds the Hashmap
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void loadBigText(File file, Map<String, Integer> nWords) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("[a-z]+");
		// Pattern p = Pattern.compile("\\w+");
		for (String temp = ""; temp != null; temp = in.readLine()) {
			Matcher m = p.matcher(temp.toLowerCase());
			String w;
			while (m.find())
				nWords.put((w = m.group()), nWords.containsKey(w) ? nWords.get(w) + 1 : 1);
		}
		in.close();
	}

	/**
	 * Reads the file, tokenizes and adds to the Hashmap - ignore anything not
	 * in the map already
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void addBigText(Map<String, Integer> nWords, File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("[a-z]+");
		for (String temp = ""; temp != null; temp = in.readLine()) {
			String w = temp.toLowerCase();
			Matcher m = p.matcher(w);
			while (m.find()) {
				temp = m.group();
				if (nWords.containsKey(temp)) {
					nWords.put(temp, nWords.get(temp) + 1);
				} else {
					// logger.debug("Ignoring {}",temp);
				}
			}
		}
		in.close();
	}

	/**
	 * Reads the file, 1-word per line, and builds tree map
	 * 
	 * @param file
	 * @throws IOException
	 */
	public Map<String, Integer> loadDict(Map<String, Integer> nWords, File file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("^[a-z]+$");
		for (String temp = ""; temp != null; temp = in.readLine()) {
			String w = temp.toLowerCase();
			Matcher m = p.matcher(w);
			if (m.matches()) {
				// logger.debug("Adding {}",temp);
				nWords.put(w, nWords.containsKey(w) ? nWords.get(w) + 1 : 1);
			} else
				logger.debug("Ignoring dict token {}", w);
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
		try (FileOutputStream fos = new FileOutputStream(path.getPath() + "/" + MODEL_NAME + ".gz")) {
			File modelFile = new File(path.getPath() + "/" + MODEL_NAME + ".txt");
			BufferedWriter br = new BufferedWriter(new FileWriter(modelFile));
			GZIPOutputStream zos = new GZIPOutputStream(fos);
			DataOutputStream dos = new DataOutputStream(zos);
			dos.writeInt(nWords.size());
			for (Entry<String, Integer> entry : nWords.entrySet()) {
				dos.writeUTF(entry.getKey());
				dos.writeInt(entry.getValue());
				br.write(entry.getKey() + "\t" + entry.getValue() + '\n');
			}
			br.flush();
			br.close();
			dos.close();
		}
	}

	/**
	 * Builds the spelling model from either a path of .txt files or 2 sources
	 * of txt. First is a list of singly occurring dictionary words. We remove
	 * any that have any special characters. Second is a general list of words.
	 * Any that match the first, we use to increment the count of the word.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		if (args.length == 2) {
			File dict = new File(args[0]);
			File bigTxt = new File(args[1]);
			File path = bigTxt.getParentFile();
			SpellingModelBuilder modelBuilder = new SpellingModelBuilder();
			Map<String, Integer> nWords = new TreeMap<String, Integer>();
			modelBuilder.loadDict(nWords, dict);
			modelBuilder.addBigText(nWords, bigTxt);
			System.out.println("Spelling model has " + nWords.size() + " words.");
			System.out.println("  Written to " + path);
			modelBuilder.exportModel(nWords, path);
		} else if (args.length == 1) { // look for text files in the path of arg
										// 1.
			File dir = new File(args[0]);
			if (dir.isDirectory()) {
				String path = dir.getParent();
				Collection<File> files = FileUtils.listFiles(dir, new String[] { "txt" }, false);
				logger.info("Building dictionary from collection of {} files from {}", files.size(), dir.getPath());
				SpellingModelBuilder modelBuilder = new SpellingModelBuilder();
				Map<String, Integer> nWords = new TreeMap<String, Integer>();
				for (File f : files) {
					modelBuilder.loadBigText(f, nWords);
				}
				System.out.println("Spelling model has " + nWords.size() + " words.");
				System.out.println("  Written to " + path);
				modelBuilder.exportModel(nWords, new File(path));
			} else {
				String path = dir.getParentFile().getParent();
				logger.info("Building dictionary from {}", dir.getPath());
				SpellingModelBuilder modelBuilder = new SpellingModelBuilder();
				Map<String, Integer> nWords = new TreeMap<String, Integer>();
				modelBuilder.loadBigText(dir, nWords);
				System.out.println("Spelling model has " + nWords.size() + " words.");
				System.out.println("  Written to " + path);
				modelBuilder.exportModel(nWords, new File(path));
			}
		} else {
			System.err.println("Name of a source text file is required as the first argument.");
		}
	}
}
