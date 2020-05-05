package org.netspeak.preprocessing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.netspeak.io.PhraseReader;

/**
 * A source of phrases.
 *
 * @see SimplePhraseSource
 */
public interface PhraseSource {

	Collection<File> getFiles() throws Exception;

	public interface File {

		Path getPath();

		PhraseReader createReader() throws Exception;

	}

	PhraseSource EMPTY = combine();

	/**
	 * Returns a phrase source which contains the files of all the given sources.
	 *
	 * @param sources
	 * @return
	 */
	static PhraseSource combine(PhraseSource... sources) {
		return combine(Arrays.asList(sources));
	}

	/**
	 * Returns a phrase source which contains the files of all the given sources.
	 *
	 * @param sources
	 * @return
	 */
	static PhraseSource combine(Collection<PhraseSource> sources) {
		final ArrayList<PhraseSource> src = new ArrayList<>(sources);
		return new Combined() {

			@Override
			public Collection<PhraseSource> getSources() {
				return src;
			}

			@Override
			public Collection<File> getFiles() throws Exception {
				List<File> files = new ArrayList<>();
				for (PhraseSource source : src) {
					files.addAll(source.getFiles());
				}
				return files;
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (PhraseSource source : src) {
					if (first) {
						first = false;
					} else {
						sb.append("\n");
					}
					sb.append(source.toString());
				}
				return sb.toString();
			}
		};
	}

	interface Combined extends PhraseSource {

		Collection<PhraseSource> getSources();

	}

}
