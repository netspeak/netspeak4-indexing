package org.netspeak.preprocessing.items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.PipelineItem;

public abstract class Operations {

	private Operations() {
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
