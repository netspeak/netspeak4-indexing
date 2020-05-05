package org.netspeak.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This CSV writer will create a given number of CSV files which will be used as
 * buckets where phrases will be assigned a bucket according to their hash.
 * These bags can then be used for further processing.
 * <p>
 * The {@link #write(String, long)} and {@link #write(PhraseFrequencyPair)}
 * methods are thread-safe.
 *
 * @author Michael
 *
 */
public class SplitterCsvWriter implements PhraseWriter {

	private final SimpleCsvWriter[] writers;
	private final Path destDir;
	private boolean initialized = false;

	public SplitterCsvWriter(Path destDir, int bucketCount) {
		this.writers = new SimpleCsvWriter[bucketCount];
		this.destDir = destDir;
	}

	@Override
	public void close() throws Exception {
		Exception last = null;

		for (SimpleCsvWriter writer : writers) {
			try {
				if (writer != null)
					writer.close();
			} catch (Exception e) {
				last = e;
			}
		}

		if (last != null)
			throw last;
	}

	@Override
	public void write(String phrase, long frequency) throws IOException {
		initializeWriters();

		int index = phrase.hashCode() % writers.length;
		if (index < 0)
			index += writers.length;
		SimpleCsvWriter writer = writers[index];
		synchronized (writer) {
			writer.write(phrase, frequency);
		}
	}

	private final void initializeWriters() throws IOException {
		if (initialized)
			return;
		synchronized (this) {
			if (initialized)
				return;

			for (int i = 0; i < writers.length; i++) {
				Path path = Paths.get(destDir.toString(), String.valueOf(i) + ".csv");
				CharsetEncoder encoder = UTF_8.newEncoder();
				Writer writer = new OutputStreamWriter(Files.newOutputStream(path, CREATE_NEW), encoder);
				writers[i] = new SimpleCsvWriter(new BufferedWriter(writer, 1024 * 256));
			}

			initialized = true;
		}
	}

}
