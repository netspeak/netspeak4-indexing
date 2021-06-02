package org.netspeak.preprocessing.items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.PipelineItem;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.mappers.PhraseMappers;

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
			final PhraseSource result = moveTo(source, dest);

			System.out.println("Done.");

			return result;
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

	private static PhraseSource moveTo(PhraseSource source, Path dest) throws Exception {
		final Set<String> names = new HashSet<>();
		boolean containsDuplicateNames = false;
		final List<PhraseSource.MovableFile> files = new ArrayList<>();

		for (final PhraseSource.File file : source.getFiles()) {
			if (file instanceof PhraseSource.MovableFile) {
				files.add((PhraseSource.MovableFile) file);

				if (!containsDuplicateNames) {
					final String name = file.getPath().getFileName().toString();
					if (names.contains(name)) {
						containsDuplicateNames = true;
					} else {
						names.add(name);
					}
				}
			} else {
				throw new Exception("File not movable: " + file.getPath().toString());
			}
		}

		int counter = 0;
		for (final PhraseSource.MovableFile file : files) {
			final int percent = counter * 100 / files.size();
			final String prefix = "[" + new Date() + "][" + percent + "% " + counter + "/" + files.size() + "] ";
			System.out.println(prefix + "Moving " + file.getPath().toString());

			String name = file.getPath().getFileName().toString();
			if (containsDuplicateNames) {
				name = counter + "-" + name;
			}

			file.move(dest.resolve(name));
			counter++;
		}

		return PhraseSource.fromFiles(new ArrayList<PhraseSource.File>(files));
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
