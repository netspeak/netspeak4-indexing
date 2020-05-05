package org.netspeak.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A reader for simple CSV files.
 * <p>
 * In these CSV files, every line ({@code \n}) contains a phrase followed by a
 * single tab ({@code \t}) followed by the frequency of that phrase. There may
 * be duplicate phrases. A phrase is a non-empty list of words each separated by
 * a single whitespace ({@code \u0020}) with no leading or trailing spaces.
 *
 * <pre>
 * hello world	20
 * i love you	100
 * hello world	5
 * </pre>
 *
 * @author Michael Schmidt
 *
 */
public class SimpleCsvReader implements PhraseReader {

	private final BufferedReader reader;

	public SimpleCsvReader(BufferedReader reader) {
		this.reader = reader;
	}

	@Override
	public PhraseFrequencyPair nextPair() throws IOException {
		String line = reader.readLine();

		if (line != null) {
			// For better performance, we avoid String#split. Instead we know that a line
			// only contains one \t, so we search for that index. To validate the format, we
			// also search for a second \t. This is equivalent to:
			// String[] parts = line.split("\t");
			// if (parts.length == 2) { create the pair } else { null }
			int firstTab = line.indexOf('\t');
			int secondTab = line.indexOf('\t', firstTab + 1);

			// The first tab has to exist and it cannot be 0 because the phrase cannot be
			// the empty string. The second tab has to not exist.
			if (firstTab > 0 && secondTab == -1) {
				String phrase = line.substring(0, firstTab);
				long frequency = Long.parseLong(line.substring(firstTab + 1));
				return new PhraseFrequencyPair(phrase, frequency);
			}
		}

		return null;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

}
