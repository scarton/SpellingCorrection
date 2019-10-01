package net.c4analytics.spelling;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * <p>
 * Spelling Checker/Corrector. Probablistic approach based on Norvic:
 * http://norvig.com/spell-correct.html Expects a previously built model with
 * correct words and their frequency of use. Assumes word is in lower case
 * 
 * Expects a "model" in the form of a .gz of a binary serialization from
 * SpellingModelBuilder. It can also handle a two models where the first is used
 * and then the second only if a correction was not found in the first. This is
 * handy of having a model that is more industry-specific used first, followed
 * by a second model that is more general. It comes from this kind of use case:
 * 
 * The word "Ankle" spelled incorrectly as Ankel is corrected to Angel (as this
 * is more probably).
 * 
 * But having a medical industry model available to check first would correct it
 * to ankle.
 * </p>
 * 
 * @author Steve Carton (stephencarton@gmail.com) Dec 22, 2015
 *
 */

final class Spelling {
	final static Logger logger = LogManager.getLogger(Spelling.class.getName());
	private String MODEL_NAME = "spelling.model";
	private String INDUSTRY_MODEL_NAME = null;

	private List<Map<String, Integer>> models;

	public Spelling() throws IOException {
		loadModels();
	}

	/**
	 * Loads and returns the spelling model from a .gz resource on the classpath
	 * 
	 * @return
	 * @throws IOException
	 */
	private final void loadModels() throws IOException {
		models = new ArrayList<>();
		if (INDUSTRY_MODEL_NAME != null) {
			try (InputStream in = Spelling.class.getResourceAsStream('/' + INDUSTRY_MODEL_NAME + ".gz")) {
				this.models.add(loadModel(in));
			}
		}
		{
			try (InputStream in = Spelling.class.getResourceAsStream('/' + MODEL_NAME + ".gz")) {
				this.models.add(loadModel(in));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Loads and returns the spelling model - a map of words and counts
	 * 
	 * @return
	 * @throws IOException
	 */
	private final Map<String, Integer> loadModel(InputStream in) throws IOException {
		Map<String, Integer> model = new HashMap<String, Integer>();
		GZIPInputStream zis = new GZIPInputStream(in);
		DataInputStream dis = new DataInputStream(zis);
		int l = dis.readInt();
		for (int i = 0; i < l; i++) {
			String k = dis.readUTF();
			Integer v = dis.readInt();
			model.put(k, v);
		}

		logger.debug("Spelling Model Loaded, {} Entries", l);
		return model;
	}

	/**
	 * <p>
	 * Creates a list of alternate word possibilities for this word. These are
	 * not necessarily correctly spelled - just alternative forms. Assumes word
	 * is in lower case There are 4 possible alternates created for the word:
	 * <ul>
	 * <li>Deletions - where one letter is removed, producing a set of words l
	 * in size. (word would create ord, wrd, wod, wor)</li>
	 * <li>Transpositions - where adjacent letters are swapped, creating word
	 * set l-1 in size (word->owrd, wrod, wodr)</li>
	 * <li>Replacements - where each letter is replaced by another a-z letter
	 * creating a word set of 26(l+1) new words. (word->aord, bord,
	 * cord...)</li>
	 * <li>Insertions - where each character has added immediately following,
	 * each of a-z, creating a word set of 54n+25.</li>
	 * </ul>
	 * Wondering if the replacements would work better if we chose from
	 * characters near the source char on the keyboard? Also wondering if we
	 * should also include numbers in the various options.
	 * </p>
	 * 
	 * @param word
	 * @return
	 */
	private static final char[] swappables = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '\'', };

	private final List<String> getWordVariants(String word) {
		char[] chars = word.toCharArray();
		List<String> result = new ArrayList<String>();
		// create word variations with 1 character deleted.
		for (int i = 0; i < chars.length; ++i) {
			// sb.append(word.substring(0, i));
			// sb.append(word.substring(i+1));
			StringBuilder sb = new StringBuilder();
			sb.append(chars, 0, i);
			sb.append(chars, i + 1, (chars.length - (i + 1)));
			result.add(sb.toString());
		}
		// Create word variations with 1 character swapped
		for (int i = 0; i < chars.length - 1; ++i) {
			StringBuilder sb = new StringBuilder();
			sb.append(chars, 0, i);
			sb.append(chars, i + 1, 1);
			sb.append(chars, i, 1);
			sb.append(chars, i + 2, chars.length - (i + 2));
			// result.add(word.substring(0, i) + word.substring(i + 1, i + 2) +
			// word.substring(i, i + 1) + word.substring(i + 2));
			result.add(sb.toString());
		}
		// create word variations with each character replaced by [a-z].
		for (int i = 0; i < chars.length; ++i) {
			for (int j = 0; j < swappables.length; j++) {
				StringBuilder sb = new StringBuilder();
				sb.append(chars, 0, i);
				sb.append(swappables[j]);
				sb.append(chars, i + 1, chars.length - (i + 1));
				// result.add(chars.substring(0, i) + String.valueOf(c) +
				// word.substring(i + 1));
				result.add(sb.toString());
			}
		}
		// create word variations with [a-z] added after each character.
		for (int i = 0; i <= chars.length; ++i) {
			for (int j = 0; j < swappables.length; j++) {
				StringBuilder sb = new StringBuilder();
				sb.append(chars, 0, i);
				sb.append(swappables[j]);
				sb.append(chars, i, chars.length - i);
				// result.add(word.substring(0, i) + String.valueOf(c) +
				// word.substring(i));
				result.add(sb.toString());
			}
		}
		return result;
	}

	/**
	 * <p>
	 * Performs spelling correction on a word. Builds a Map of candidates by
	 * creating the word variants and then removing those that are not real
	 * words. Returns the word most probably the correction. Norvik postulates
	 * running each word in the variants list through the edits function to
	 * account for character errors that are 2 apart.
	 * </p>
	 * 
	 * @param word
	 * @return
	 */
	public final String correct(String word, Map<String, Integer> dictionary) {
		if (dictionary.containsKey(word) || word.length()>50) {
			return word;
		} 
		// get a list of all variants
		List<String> wordVariants = getWordVariants(word);
		// this will hold a subset of the model - words in the variants that are
		// in the model (are actual words) and their frequency score.
		TreeMap<Integer, String> candidates = new TreeMap<Integer, String>();
		for (String wordVariant : wordVariants) {
			Integer wordFrequency = 0;
			if (dictionary.containsKey(wordVariant)) {
				wordFrequency = dictionary.get(wordVariant);
				candidates.put(wordFrequency, wordVariant);
			}
		}
		// If we found candidates, we return the one with the highest
		// score/frequency

		if (candidates.size() > 0) {
			String correctedWord = candidates.lastEntry().getValue();
			return correctedWord;
		}
		// Still no joy, go through the candidates again and create variants on
		// the variants (Norviks postulate above).
		// Add any real words and their frequency to the Map
		for (String wordVariant : wordVariants) {
			List<String> secondaryWordVariants = getWordVariants(wordVariant);
			for (String secondaryWordVariant : secondaryWordVariants) {
				Integer wordFrequency = dictionary.get(secondaryWordVariant);
				if (dictionary.containsKey(secondaryWordVariant)) {
					candidates.put(wordFrequency, secondaryWordVariant);
				}
			}
		}
		// If we have something n the map, return the word with the highest
		// score. Otherwise return the unchanged word.
		String correctedWord = null;
		if (candidates.size() > 0) {
			correctedWord = candidates.lastEntry().getValue();
		}
		return candidates.size() > 0 ? correctedWord : word;
	}

	/**
	 * Is this word in the dictionary?
	 * 
	 * @param word
	 * @return
	 */
	public final boolean hasWord(String word) {
		return models.get(0).containsKey(word);
	}

	/**
	 * <p>
	 * Corrects spelling of an array of words. Assumes the words are in lower
	 * case.
	 * 
	 * Tries the "industry" model first. If that dowsn't cause a fix to the
	 * word, tries the general model. If there is only one model, that's the
	 * only one used. case.
	 * </p>
	 * * @param words
	 * 
	 * @return
	 */
	/**
	 * @param words
	 * @return
	 */
	public final String[] correct(String... words) {
		ArrayList<String> fixed = new ArrayList<String>();
		for (String word : words) {
			fixed.add(correct(word, models.get(0)));
		}
		return fixed.toArray(new String[0]);
	}

	/**
	 * <p>
	 * To test with... supply words to check as args to the
	 * </p>
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		Spelling spelling = new Spelling();
		spelling.loadModels();
		if (args.length > 0) {
			String[] words = args;
			String[] cwds = spelling.correct(words);
			for (int i = 0; i < words.length; i++)
				System.out.println("'" + words[i] + "' - '" + cwds[i] + "'");
		}
	}

	public void setMODEL_NAME(String m) {
		MODEL_NAME = m;
	}

	public void setINDUSTRY_MODEL_NAME(String im) {
		INDUSTRY_MODEL_NAME = im;
	}
}
