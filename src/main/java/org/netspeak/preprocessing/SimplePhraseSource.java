package org.netspeak.preprocessing;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.netspeak.io.PhraseReader;
import org.netspeak.io.SimpleCsvReader;

public class SimplePhraseSource implements PhraseSource {

	private final Path path;
	private PhraseReaderFactory readerFactory = SimpleCsvReader::new;
	private Filter<Path> fileFilter;

	public SimplePhraseSource(Path path) {
		this.path = requireNonNull(path);
	}

	public SimplePhraseSource(String path) {
		this.path = Paths.get(requireNonNull(path));
	}

	@Override
	public String toString() {
		return path.toString();
	}

	/**
	 * Sets the factory to create a new {@link PhraseReader} from the given
	 * {@link BufferedReader}.
	 * <p>
	 * This defaults to {@code NetspeakCsvReader::new}.
	 *
	 * @param readerFactory
	 */
	public void setReaderFactory(PhraseReaderFactory readerFactory) {
		this.readerFactory = requireNonNull(readerFactory);
	}

	/**
	 * Sets a filter which decides whether a file will be processed.
	 * <p>
	 * This defaults to {@code null} meaning that all files in the given directory
	 * will be processed.
	 *
	 * @param fileFilter
	 */
	public void setFileFilter(Filter<Path> fileFilter) {
		this.fileFilter = fileFilter;
	}

	/**
	 * Sets a glob pattern which decides whether a file will be processed.
	 * <p>
	 * This defaults to {@code null} meaning that all files in the given directory
	 * will be processed.
	 *
	 * @param globPattern
	 */
	public void setFileFilter(String globPattern) {
		if (globPattern == null) {
			this.fileFilter = null;
		} else {
			final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(globPattern);
			this.fileFilter = pathMatcher::matches;
		}
	}

	@Override
	public Collection<PhraseSource.File> getFiles() throws Exception {
		if (!Files.isDirectory(path)) {
			throw new AssertionError("Not a directory " + path);
		}

		final List<PhraseSource.File> files = new ArrayList<>();

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				if (fileFilter == null || fileFilter.accept(path)) {
					files.add(new SimplePhraseSourceFile(path, readerFactory));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return files;
	}

}
