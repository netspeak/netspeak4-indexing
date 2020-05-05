package org.netspeak.preprocessing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.netspeak.io.PhraseReader;
import org.netspeak.io.SimpleCsvReader;

public class SimplePhraseSource implements PhraseSource {

	final Path path;
	Function<BufferedReader, PhraseReader> readerFactory = SimpleCsvReader::new;
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
	public void setReaderFactory(Function<BufferedReader, PhraseReader> readerFactory) {
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

		List<PhraseSource.File> files = new ArrayList<>();
		SimplePhraseSource that = this;

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				if (fileFilter == null || fileFilter.accept(path)) {
					files.add(new PhrasesSourceFile(that, path));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return files;
	}

	private static class PhrasesSourceFile implements PhraseSource.File {

		private final SimplePhraseSource source;
		private final Path path;

		public PhrasesSourceFile(SimplePhraseSource source, Path path) {
			this.source = requireNonNull(source);
			this.path = requireNonNull(path);
		}

		@Override
		public Path getPath() {
			return path;
		}

		@Override
		public String toString() {
			return path.toString();
		}

		@Override
		public PhraseReader createReader() throws Exception {
			BufferedReader br;

			String lowerPath = path.toString().toLowerCase();
			if (lowerPath.endsWith(".zip")) {
				br = createZipReader();
			} else if (lowerPath.endsWith(".bz2")) {
				br = createBz2Reader();
			} else if (lowerPath.endsWith(".gz")) {
				br = createGZipReader();
			} else {
				br = Files.newBufferedReader(path, UTF_8);
			}

			try {
				return source.readerFactory.apply(br);
			} catch (Throwable e) {
				br.close();
				throw e;
			}
		}

		private BufferedReader createZipReader() throws Exception {
			// we assume that the .zip contains only one file which is a CSV file
			BufferedInputStream bis = null;
			ZipInputStream zip = null;
			try {
				bis = new BufferedInputStream(Files.newInputStream(path));
				zip = new ZipInputStream(bis);
				ZipEntry entry = zip.getNextEntry();
				if (entry == null) {
					throw new IllegalStateException("The .zip file is empty.");
				}
				if (!entry.getName().toLowerCase().endsWith(".csv")) {
					throw new IllegalStateException("The .zip file is only allowed to contain a CSV file.");
				}
				return new BufferedReader(new InputStreamReader(zip, UTF_8));
			} catch (Throwable t) {
				if (bis != null)
					bis.close();
				if (zip != null)
					zip.close();
				throw t;
			}
		}

		private BufferedReader createBz2Reader() throws Exception {
			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(Files.newInputStream(path));
				return new BufferedReader(
						new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(bis), UTF_8));
			} catch (Throwable t) {
				if (bis != null)
					bis.close();
				throw t;
			}
		}

		private BufferedReader createGZipReader() throws IOException {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), UTF_8));
		}

	}

}
