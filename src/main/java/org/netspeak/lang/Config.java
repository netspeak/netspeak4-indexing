package org.netspeak.lang;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.PreprocessingOptions;

public final class Config extends MapperConfig {

	public final PhraseSource source;
	public final Path output;

	public Path temp;
	public int parallelDegree = 1;
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

	public PreprocessingOptions getPreprocessingOptions() {
		final PreprocessingOptions options = new PreprocessingOptions();
		options.setParallelDegree(parallelDegree);
		options.setMergeDuplicates(mergeDuplicates);
		return options;
	}
}
