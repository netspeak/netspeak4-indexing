package org.netspeak.lang;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import org.netspeak.preprocessing.PhraseSource;

public final class Config {

	public final PhraseSource source;
	public final Path output;
	public final Path temp;

	public int parallelDegree = 1;
	public int maxNGram = Integer.MAX_VALUE;
	public boolean lowercase = false;

	public Config(PhraseSource source, Path output, Path temp) {
		this.source = requireNonNull(source);
		this.output = requireNonNull(output);
		this.temp = requireNonNull(temp);
	}
}
