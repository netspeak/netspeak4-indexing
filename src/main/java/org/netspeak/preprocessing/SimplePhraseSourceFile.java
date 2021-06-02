package org.netspeak.preprocessing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.netspeak.io.PhraseReader;
import org.netspeak.io.SimpleCsvReader;

public class SimplePhraseSourceFile implements PhraseSource.MovableFile {

	private Path path;
	private final Format format;
	private final PhraseReaderFactory readerFactory;

	public SimplePhraseSourceFile(Path path) {
		this(path, SimpleCsvReader::new);
	}

	public SimplePhraseSourceFile(Path path, PhraseReaderFactory readerFactory) {
		this(path, readerFactory, detectFormat(path));
	}

	public SimplePhraseSourceFile(Path path, PhraseReaderFactory readerFactory, Format format) {
		this.path = requireNonNull(path);
		this.readerFactory = requireNonNull(readerFactory);
		this.format = requireNonNull(format);
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public void move(Path to) throws Exception {
		Files.move(path, to);
		path = to;
	}

	@Override
	public String toString() {
		return path.toString();
	}

	@Override
	public PhraseReader createReader() throws Exception {
		final BufferedReader br = read(Files.newInputStream(path), format);

		try {
			return readerFactory.createReader(br);
		} catch (final Throwable e) {
			br.close();
			throw e;
		}
	}

	private static Format detectFormat(Path path) {
		final String lowerPath = path.getFileName().toString().toLowerCase();

		if (lowerPath.endsWith(".zip")) {
			return Format.ZIP;
		} else if (lowerPath.endsWith(".bz2")) {
			return Format.BZ2;
		} else if (lowerPath.endsWith(".gz")) {
			return Format.GZIP;
		} else if (lowerPath.endsWith(".csv")) {
			return Format.TEXT;
		}

		throw new RuntimeException("Unable to detect file format for " + path.toString());
	}

	private static BufferedReader read(InputStream in, Format format) throws Exception {
		switch (format) {
		case TEXT:
			return new BufferedReader(new InputStreamReader(in, UTF_8));
		case GZIP:
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(in), UTF_8));
		case BZ2:
			return readBZ2(in);
		case ZIP:
			return readZip(in);
		default:
			throw new IllegalArgumentException("Unknown format");
		}
	}

	private static BufferedReader readBZ2(InputStream in) throws Exception {
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(in);
			return new BufferedReader(
					new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(bis), UTF_8));
		} catch (final Throwable t) {
			if (bis != null)
				bis.close();

			throw t;
		}
	}

	private static BufferedReader readZip(InputStream in) throws Exception {
		// we assume that the .zip contains only one file which is a CSV file
		BufferedInputStream bis = null;
		ZipInputStream zip = null;
		try {
			bis = new BufferedInputStream(in);
			zip = new ZipInputStream(bis);
			final ZipEntry entry = zip.getNextEntry();
			if (entry == null) {
				throw new IllegalStateException("The .zip file is empty.");
			}
			if (!entry.getName().toLowerCase().endsWith(".csv")) {
				throw new IllegalStateException("The .zip file is only allowed to contain a single CSV file.");
			}
			return new BufferedReader(new InputStreamReader(zip, UTF_8));
		} catch (final Throwable t) {
			if (bis != null)
				bis.close();
			if (zip != null)
				zip.close();
			throw t;
		}
	}

	public enum Format {
		TEXT, GZIP, BZ2, ZIP
	}

}
