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

	public interface MovableFile extends File {

		void move(Path to) throws Exception;

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
		return new PhraseSource() {

			@Override
			public Collection<File> getFiles() throws Exception {
				final List<File> files = new ArrayList<>();
				for (final PhraseSource source : src) {
					files.addAll(source.getFiles());
				}
				return files;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (final PhraseSource source : src) {
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

	/**
	 * Returns a phrase source which contains all the given files.
	 *
	 * @param files
	 * @return
	 */
	static PhraseSource fromFiles(File... files) {
		return fromFiles(Arrays.asList(files));
	}

	/**
	 * Returns a phrase source which contains all the given files.
	 *
	 * @param files
	 * @return
	 */
	static PhraseSource fromFiles(Collection<File> files) {
		final ArrayList<File> f = new ArrayList<>(files);

		return new PhraseSource() {

			@Override
			public Collection<File> getFiles() throws Exception {
				return f;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("Files:");
				for (final File file : f) {
					sb.append(file.getPath().toString());
				}
				return sb.toString();
			}
		};
	}

}
