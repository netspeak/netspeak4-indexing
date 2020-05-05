package org.netspeak.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A reader for Google books CSV files.
 * <p>
 * These files are a bit difficult to parse because the n-grams are also
 * separated by years. So there will be many consecutive occurrences of the same
 * phrase but for different years. This reader will automatically parse and
 * aggregate these entries.
 * <p>
 * Example:
 *
 * <pre>
 * collision such    2000    4     4     4
 * collision such    2001    6     6     6
 * collision such    2002    6     6     6
 * collision such    2003    10    11    0
 * collision such    2004    17    11    5
 * collision such    2005    14    11    3
 * collision such    2006    20    22    0
 * collision such    2007    17    11    7
 * collision such    2008    19    11    8
 * </pre>
 *
 * These will all be parsed and aggregated into:
 *
 * <pre>
 * <code>
 * String phrase  = "collision such";
 * long frequency = 113;
 * </code>
 * </pre>
 *
 * @author Michael Schmidt
 *
 */
public class GoogleBooksCsvReader implements PhraseReader {

	private final BufferedReader reader;
	private String lastLine = null;

	public GoogleBooksCsvReader(BufferedReader reader) {
		this.reader = reader;
	}

	@Override
	public PhraseFrequencyPair nextPair() throws IOException {
		String line = lastLine == null ? reader.readLine() : lastLine;

		MutablePhraseFrequencyPair pair = new MutablePhraseFrequencyPair(null, -1);
		while (line != null && !parseLine(line, pair)) {
			// we read lines until we find one which parses or arrive at the end
			line = reader.readLine();
		}
		if (line == null || pair.phrase == null)
			return null;

		// aggregate the frequencies of the next lines which also have the current
		// phrase
		String currentPhrase = pair.phrase;
		long currentFrequency = pair.frequency;

		String nextLine;
		while ((nextLine = reader.readLine()) != null) {
			if (parseLine(nextLine, pair)) {
				if (currentPhrase.contentEquals(pair.phrase)) {
					currentFrequency += pair.frequency;
				} else {
					break;
				}
			}
		}
		lastLine = nextLine;

		return new PhraseFrequencyPair(currentPhrase, currentFrequency);
	}

	/**
	 * This parses a CSV line.
	 * <p>
	 * Returns {@code false} if the given line could not be parsed.
	 */
	private static boolean parseLine(String line, MutablePhraseFrequencyPair pair) {
		// e.g. "circumvallate\t1978\t313\t215\t85"
		// "The first line tells us that in 1978, the word "circumvallate" occurred 313
		// times overall, on 215 distinct pages and in 85 distinct books."

		// this operation will be done millions of times, so I want to avoid
		// String#split

		int firstTab = line.indexOf('\t', 0);
		int secondTab = line.indexOf('\t', firstTab + 1);
		int thirdTab = line.indexOf('\t', secondTab + 1);
		if (firstTab == -1 || secondTab == -1 || thirdTab == -1)
			return false;

		// phrases sometimes have a trailing space, so we have to remove that
		String phrase = line.substring(0, firstTab).trim();
		// the empty string is not a valid phrase
		if (phrase.isEmpty()) {
			return false;
		}

		pair.phrase = phrase;
		pair.frequency = Long.parseLong(line.substring(secondTab + 1, thirdTab));

		return true;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	private static class MutablePhraseFrequencyPair {

		public String phrase;
		public long frequency;

		public MutablePhraseFrequencyPair(final String phrase, final long frequency) {
			this.phrase = phrase;
			this.frequency = frequency;
		}

	}

}
