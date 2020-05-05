package org.netspeak.preprocessing;

import org.netspeak.Util;
import org.netspeak.Util.ThrowsConsumer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * This will join all hyphenated words in two phases.
 * <p>
 * In the first pass it will iterate over all input phrases and extract the vocabulary and stop words. This will be done
 * according to the given options.
 * <p>
 * The second phase is specific to the joiner set. Generally, it will try to normalize and, where possible, join
 * hyphenated words.
 *
 * @see German
 */
public class HyphenationJoiner implements PipelineItem {

	private Path logFile;
	private ThrowsConsumer<VocabularyExtractor> vocabularyConsumer;

	private final Joiner joiner;
	private final Path output;
	private final PreprocessingOptions options;

	public HyphenationJoiner(JoinerFactory joinerFactory, Path output, PreprocessingOptions options) throws Exception {
		this.joiner = requireNonNull(joinerFactory).create();
		this.output = requireNonNull(output);
		this.options = requireNonNull(options);
	}

	/**
	 * Sets an optional log file.
	 * <p>
	 * Every action the method takes will be logged here in order where each line is one operation. The result and the
	 * reason for not joining will be in the following format:
	 *
	 * <pre>
	 * {action}:[ {result}:] {phrase}
	 * </pre>
	 * <p>
	 * The actions are and results are implementation defined and might be different for each language specific joiner.
	 * See the implementation of the current joiner for more information.
	 */
	public void setLogger(Path logFile) {
		this.logFile = logFile;
	}

	public void setVocabularyConsumer(ThrowsConsumer<VocabularyExtractor> vocabularyConsumer) {
		this.vocabularyConsumer = vocabularyConsumer;
	}

	@Override
	public PhraseSource apply(PhraseSource source) throws Exception {
		// Pass 1

		if (joiner.getRequiresVocabulary()) {
			System.out.println("Extracting vocabulary...");

			VocabularyExtractor vocabExtractor = new VocabularyExtractor();
			Preprocessing.iterate(source, Arrays.asList(vocabExtractor), options);

			System.out.println("Preparing vocabulary...");
			if (vocabularyConsumer != null) {
				vocabularyConsumer.accept(vocabExtractor);
			}

			Set<String> vocabulary = vocabExtractor.getVocabulary();
			vocabExtractor = null;
			System.gc();

			joiner.setVocabulary(vocabulary);
		}

		System.out.println("Joining Hyphenations...");
		options.setMergeDuplicates(true); // this operation is going to create duplicates

		// We use Java 8, so we have to give it a charset name, so it can lookup the charset instance. In never version
		// you can give it an instance directly.
		String charsetName = StandardCharsets.UTF_8.name();
		final PrintStream logger = logFile == null ? null : new PrintStream(logFile.toFile(), charsetName);
		try {
			joiner.setLogger(logger);

			return Preprocessing.process(source, output, Arrays.asList(joiner), options);
		} finally {
			if (logger != null) {
				logger.close();
			}
		}
	}

	public interface Joiner extends PhraseMapper {
		boolean getRequiresVocabulary();

		void setVocabulary(Set<String> vocabulary);

		void setLogger(PrintStream logger);
	}

	public interface JoinerFactory {
		Joiner create() throws Exception;
	}

	public static class German implements JoinerFactory {
		/**
		 * The top k words from the vocabulary will be treated as stop words. This will set the k.
		 */
		int stopWordsTopK = 100;
		/**
		 * An optional stop word list.
		 * <p>
		 * This list will be merged with the top k stop words from the vocabulary.
		 */
		Collection<String> stopWordList = null;

		public void setStopWordsTopK(int stopWordsTopK) {
			this.stopWordsTopK = stopWordsTopK;
		}

		public void setStopWordList(Path stopWordList) throws IOException {
			this.stopWordList = Util.readWordList(stopWordList);
		}

		public void setStopWordList(Collection<String> stopWordList) {
			this.stopWordList = stopWordList;
		}

		@Override
		public Joiner create() throws Exception {
			return new GermanJoiner(this);
		}
	}

	private static class GermanJoiner implements Joiner {

		private final German options;

		private Set<String> vocabulary;
		private Set<String> stopWords = new HashSet<>();

		private PrintStream logger;

		public GermanJoiner(German options) throws IOException {
			this.options = requireNonNull(options);
			if (options.stopWordList != null)
				stopWords.addAll(options.stopWordList);
		}

		@Override
		public void setVocabulary(Set<String> vocabulary) {
			this.vocabulary = vocabulary;
			HyphenationJoiner.addTopK(stopWords, vocabulary, options.stopWordsTopK);
		}

		@Override
		public void setLogger(PrintStream logger) {
			this.logger = logger;
		}

		@Override
		public boolean getRequiresVocabulary() {
			return true;
		}

		private String[] normalizeHyphens(String[] words, String phrase) {
			if (words.length < 2)
				return words;

			int toRemove = 0;
			for (int i = 1; i < words.length; i++) {
				if ("-".contentEquals(words[i]))
					toRemove++;
			}

			if (toRemove == 0)
				return words;

			String[] newWords = new String[words.length - toRemove];
			newWords[0] = words[0];
			int writeIndex = 1;
			for (int i = 1; i < words.length; i++) {
				String word = words[i];
				if ("-".contentEquals(words[i])) {
					newWords[writeIndex - 1] = newWords[writeIndex - 1] + "-";
				} else {
					newWords[writeIndex++] = word;
				}
			}

			if (logger != null) {
				logger.println("Normalize: " + Util.toPhrase(newWords) + ": " + phrase);
			}

			return newWords;
		}

		private String[] joinHyphen(String[] words, String phrase) {
			/**
			 * For all pairs matching the pattern `{words1}- {words2}`, we want to transform
			 * them to either `{words1}{words2}`, `{words1}-{words2}`, or leave them as is.
			 */

			for (int i = 0; i < words.length - 1; i++) {
				String word = words[i];
				String next = words[i + 1];
				if (word.length() > 1 && word.charAt(word.length() - 1) == '-') {

					// if the next word is a stop word, we leave it as is.
					if (stopWords.contains(next)) {
						if (logger != null) {
							logger.println("Stop word: " + next + ": " + phrase);
						}
						continue;
					}

					String result = null;

					/**
					 * To do the word join {word1}{word2}, 3 criteria have to be met:
					 *
					 * 1. {word2} can't be a stop word. <br>
					 * 2. {word2} has to begin with a lower case letter. <br>
					 * 3. The concatenation {word1}{word2} has to be a known word.
					 */

					if (Character.isLowerCase(next.charAt(0))) {
						String concat = word.substring(0, word.length() - 1) + next;
						if (vocabulary.contains(concat)) {
							result = concat;
							if (logger != null) {
								logger.println("Full join: " + concat + ": " + phrase);
							}
						}
					}

					words[i] = null;
					words[i + 1] = result == null ? word + next : result;
				}
			}

			return HyphenationJoiner.removeNull(words);
		}

		@Override
		public String map(String phrase, long frequency) {
			if (phrase.indexOf('-') == -1)
				return phrase;

			String[] words = normalizeHyphens(phrase.split(" "), phrase);

			words = joinHyphen(words, phrase);

			return Util.toPhrase(words);
		}

	}

	public static class English implements JoinerFactory {
		@Override
		public Joiner create() throws Exception {
			return new EnglishJoiner();
		}
	}

	private static class EnglishJoiner implements Joiner {

		private PrintStream logger;

		@Override
		public void setVocabulary(Set<String> vocabulary) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLogger(PrintStream logger) {
			this.logger = logger;
		}

		@Override
		public boolean getRequiresVocabulary() {
			return false;
		}

		@Override
		public String map(String phrase, long frequency) {
			if (phrase.indexOf(" - ") == -1)
				return phrase;

			String newPhrase = phrase.replace(" - ", "-");
			if (logger != null) {
				logger.println("Join: " + newPhrase + ": " + phrase);
			}

			return newPhrase;
		}

	}

	private static String[] removeNull(String[] words) {
		int nullEntries = 0;
		for (String word : words)
			if (word == null)
				nullEntries++;

		if (nullEntries == 0)
			return words;

		String[] newWords = new String[words.length - nullEntries];
		int writeIndex = 0;
		for (String w : words) {
			if (w != null)
				newWords[writeIndex++] = w;
		}
		return newWords;
	}

	private static <T> void addTopK(Collection<T> consumer, Collection<T> supplier, int k) {
		for (T item : supplier) {
			if (k-- <= 0)
				break;
			consumer.add(item);
		}
	}

}
