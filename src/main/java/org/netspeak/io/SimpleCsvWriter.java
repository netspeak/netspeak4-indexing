package org.netspeak.io;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * A writer for CSV files which can be understood by the Netspeak index builder.
 * <p>
 * For more details on the format see {@link SimpleCsvReader}.
 *
 * @author Michael Schmidt
 */
public class SimpleCsvWriter implements PhraseWriter {

	private final BufferedWriter writer;

	public SimpleCsvWriter(BufferedWriter writer) {
		this.writer = writer;
	}

	@Override
	public void write(String phrase, long frequency) throws IOException {
		writer.append(phrase).append('\t').append(Long.toString(frequency)).append('\n');
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
