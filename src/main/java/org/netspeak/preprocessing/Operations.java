package org.netspeak.preprocessing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.netspeak.Util;

public abstract class Operations {

	private Operations() {
	}

	public static PipelineItem standardOperations(Path output, StandardOperationsOptions operationOptions,
			PreprocessingOptions options) {
		return source -> {
			final List<PhraseMapper> mappers = new ArrayList<>();

			// try to remove as much junk as possible
			// In this phase, phrases will only be removed and not altered.
			mappers.add(PhraseMappers.removeControlCharacters());
			if (operationOptions.superBlacklist != null) {
				mappers.add(PhraseMappers.superBlacklist(operationOptions.superBlacklist));
			}
			mappers.add(PhraseMappers.removeGoogleWebMarkers());
			mappers.add(PhraseMappers.removeHTMLEntities());
			mappers.add(PhraseMappers.removeURLsAndEmails());
			mappers.add(PhraseMappers.removeFileNames());

			// Normalization phase
			mappers.add(PhraseMappers.normalizeApostrophe());
			mappers.add(PhraseMappers.normalizeHyphens());
			mappers.add(PhraseMappers.explodeCommas());
			mappers.add(PhraseMappers.removeLeadingDoubleQuote());
			mappers.add(PhraseMappers.joinWordsWithLeadingApostrophe());

			if (operationOptions.blacklist != null) {
				mappers.add(
						PhraseMappers.blacklist(operationOptions.blacklist, operationOptions.blacklistCombinations));
			}
			if (operationOptions.maxNGram < Integer.MAX_VALUE) {
				mappers.add(PhraseMappers.maxNGram(operationOptions.maxNGram));
			}
			if (operationOptions.toLowerCase) {
				mappers.add(PhraseMappers.toLowerCase());
			}

			if (operationOptions.additionalMappers != null) {
				mappers.addAll(operationOptions.additionalMappers);
			}

			// the above operations are going to produce duplicates
			options.setMergeDuplicates(true);

			return Preprocessing.process(source, output, mappers, options);
		};
	}

	public static class StandardOperationsOptions {
		/**
		 * The maximum allowed number of words per phrase.
		 */
		int maxNGram = Integer.MAX_VALUE;
		/**
		 * Whether all phrases should be lower-cased.
		 */
		boolean toLowerCase = false;
		/**
		 * All phrases with at least one word which can be constructed from at most
		 * {@link #blacklistCombinations} many blacklisted word will be removed.
		 */
		Collection<String> blacklist = null;
		int blacklistCombinations = 4;
		/**
		 * @see PhraseMappers#superBlacklist(Iterable)
		 */
		Collection<String> superBlacklist = null;
		/**
		 * Additional mappers which will be executed after the mappers defined by the
		 * method.
		 */
		List<PhraseMapper> additionalMappers = new ArrayList<>();

		public void setBlacklist(Path blacklist) throws IOException {
			this.blacklist = Util.readWordList(blacklist);
		}

		public void setBlacklist(Collection<String> blacklist) {
			this.blacklist = blacklist;
		}

		public void setBlacklistCombinations(int blacklistCombinations) {
			this.blacklistCombinations = blacklistCombinations;
		}

		public void setSuperBlacklist(Path superBlacklist) throws IOException {
			this.superBlacklist = Util.readWordList(superBlacklist);
		}

		public void setSuperBlacklist(Collection<String> superBlacklist) {
			this.superBlacklist = superBlacklist;
		}

		public List<PhraseMapper> getAdditionalMappers() {
			return additionalMappers;
		}

		public void setToLowerCase(boolean toLowerCase) {
			this.toLowerCase = toLowerCase;
		}

		public void setMaxNGram(int maxNGram) {
			this.maxNGram = maxNGram;
		}
	}

	/**
	 * Moves all files to the given directory.
	 *
	 * @param output The directory to move to.
	 * @return
	 */
	public static PipelineItem moveTo(Path output) {
		return source -> {
			final Path dest = output.toAbsolutePath();
			System.out.println("Moving to " + dest);
			System.out.println("From:");
			System.out.println(source);

			Util.createEmptyDirectory(dest);
			final List<PhraseSource> newSources = new ArrayList<>();
			moveTo(newSources, source, dest);

			System.out.println("Done.");

			if (newSources.size() == 1) {
				return newSources.get(0);
			} else {
				return PhraseSource.combine(newSources);
			}
		};
	}

	/**
	 * Moves all files to the given directory.
	 *
	 * @param output The directory to move to.
	 * @return
	 */
	public static PipelineItem moveTo(String output) {
		return moveTo(Paths.get(output));
	}

	private static void moveTo(List<PhraseSource> out, PhraseSource source, Path dest) throws Exception {
		if (source instanceof PhraseSource.Combined) {
			for (final PhraseSource s : ((PhraseSource.Combined) source).getSources()) {
				moveTo(out, s, dest);
			}
		} else if (source instanceof SimplePhraseSource) {
			final SimplePhraseSource simple = (SimplePhraseSource) source;
			// actually move some files
			for (final PhraseSource.File file : simple.getFiles()) {
				Files.move(file.getPath(), dest.resolve(file.getPath().getFileName()));
			}

			final SimplePhraseSource newSource = new SimplePhraseSource(dest);
			newSource.setReaderFactory(simple.readerFactory);
			out.add(newSource);
		} else {
			throw new UnsupportedOperationException(
					"Cannot move files of unknown source class " + source.getClass().getName());
		}
	}

	/**
	 * Deletes all files of the input phrase source.
	 * <p>
	 * The item will return {@link PhraseSource#EMPTY}.
	 *
	 * @return
	 */
	public static PipelineItem delete() {
		return source -> {
			System.out.println("Deleting:");
			System.out.println(source);

			for (final PhraseSource.File file : source.getFiles()) {
				Files.delete(file.getPath());
			}

			System.out.println("Done.");
			return PhraseSource.EMPTY;
		};
	}

}
