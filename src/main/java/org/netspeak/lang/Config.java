package org.netspeak.lang;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseSource;

public final class Config {

	public final PhraseSource source;
	public final Path output;

	public Path temp;
	public int parallelDegree = 1;
	public int maxNGram = Integer.MAX_VALUE;
	public boolean lowercase = false;
	public boolean mergeDuplicates = true;

	public Config(PhraseSource source, Path output) {
		this.source = requireNonNull(source);
		this.output = requireNonNull(output);
	}

	/**
	 * Returns the path of a temporary directory guaranteed not to exist.
	 *
	 * @return
	 * @throws IOException
	 */
	public Path newTempDir() throws IOException {
		return Util.getTempDir(temp == null ? output : temp);
	}
}
