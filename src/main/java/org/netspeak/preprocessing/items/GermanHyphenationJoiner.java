package org.netspeak.preprocessing.items;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.PipelineItem;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.mappers.VocabularyExtractor;

/**
 * This will join all hyphenated words in two phases.
 * <p>
 * In the first pass it will iterate over all input phrases and extract the
 * vocabulary and stop words. This will be done according to the given options.
 * <p>
 * The second phase is specific to the joiner set. Generally, it will try to
 * normalize and, where possible, join hyphenated words.
 */
public class GermanHyphenationJoiner implements PipelineItem {

	private final Joiner joiner;
	private final Path output;
	private final PreprocessingOptions options;

	public GermanHyphenationJoiner(Set<String> stopWordList, Path output, PreprocessingOptions options) throws Exception {
		this.joiner = new Joiner(stopWordList);
		this.output = requireNonNull(output);
		this.options = requireNonNull(options);
	}

	@Override
	public PhraseSource apply(PhraseSource source) throws Exception {
		// Pass 1

		System.out.println("Extracting vocabulary...");

		VocabularyExtractor vocabExtractor = new VocabularyExtractor();
		Preprocessing.iterate(source, Arrays.asList(vocabExtractor), options);

		System.out.println("Preparing vocabulary...");

		final Set<String> vocabulary = vocabExtractor.getVocabulary();
		vocabExtractor = null;
		System.gc();

		joiner.setVocabulary(vocabulary);

		System.out.println("Joining Hyphenations...");

		return Preprocessing.process(source, output, Arrays.asList(joiner), options);
	}

	private static class Joiner implements PhraseMapper {

		private Set<String> vocabulary;
		private final Set<String> stopWords = new HashSet<>();

		public Joiner(Set<String> stopWordList) throws IOException {
			if (stopWordList != null) {
				stopWords.addAll(stopWordList);
			}
		}

		public void setVocabulary(Set<String> vocabulary) {
			this.vocabulary = vocabulary;

			// add the top 100 words as stop words
			vocabulary.stream().limit(100).forEach(stopWords::add);
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

			final String[] newWords = new String[words.length - toRemove];
			newWords[0] = words[0];
			int writeIndex = 1;
			for (int i = 1; i < words.length; i++) {
				final String word = words[i];
				if ("-".contentEquals(words[i])) {
					newWords[writeIndex - 1] = newWords[writeIndex - 1] + "-";
				} else {
					newWords[writeIndex++] = word;
				}
			}

			return newWords;
		}

		private String[] joinHyphen(String[] words, String phrase) {
			/**
			 * For all pairs matching the pattern `{words1}- {words2}`, we want to transform
			 * them to either `{words1}{words2}`, `{words1}-{words2}`, or leave them as is.
			 */

			for (int i = 0; i < words.length - 1; i++) {
				final String word = words[i];
				final String next = words[i + 1];
				if (word.length() > 1 && word.charAt(word.length() - 1) == '-') {

					// if the next word is a stop word, we leave it as is.
					if (stopWords.contains(next)) {
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
						final String concat = word.substring(0, word.length() - 1) + next;
						if (vocabulary.contains(concat)) {
							result = concat;
						}
					}

					words[i] = null;
					words[i + 1] = result == null ? word + next : result;
				}
			}

			return GermanHyphenationJoiner.removeNull(words);
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

	private static String[] removeNull(String[] words) {
		int nullEntries = 0;
		for (final String word : words)
			if (word == null)
				nullEntries++;

		if (nullEntries == 0)
			return words;

		final String[] newWords = new String[words.length - nullEntries];
		int writeIndex = 0;
		for (final String w : words) {
			if (w != null)
				newWords[writeIndex++] = w;
		}
		return newWords;
	}

}
