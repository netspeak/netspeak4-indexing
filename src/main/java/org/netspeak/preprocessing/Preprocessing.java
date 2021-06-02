package org.netspeak.preprocessing;

import org.netspeak.Util;
import org.netspeak.Util.ThrowsRunnable;
import org.netspeak.io.PhraseFrequencyPair;
import org.netspeak.io.PhraseReader;
import org.netspeak.io.PhraseWriter;
import org.netspeak.io.SimpleCsvReader;
import org.netspeak.io.SimpleCsvWriter;
import org.netspeak.io.SplitterCsvWriter;
import org.netspeak.preprocessing.PreprocessingOptions.DeleteMode;
import org.netspeak.preprocessing.mappers.PhraseMappers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.DAYS;

/**
 * <p>
 * A class to convert a number of input phrase files to a number of output phrase files by applying user-defined filters
 * on each input phrase.
 * </p>
 *
 * <p>
 * For that reason the user can register classes that implement the {@link PhraseMapper} interface to provide a certain
 * filter functions. All filters will then be applied on each phrase in the same order they were registered. Some
 * predefined {@link PhraseMapper} can be found in the {@link PhraseMappers} class.
 * </p>
 */
public final class Preprocessing {

	private Preprocessing() {
	}

	/**
	 * Runs the entire preprocessing step which applies a number of filters on each phrase read from files located in
	 * {@code phraseSrcDir}. As a precondition all files in {@code phraseSrcDir} must be formatted according to the
	 * phrase file format as defined in {@code netspeak3-application-java-notes.txt}.
	 * <p>
	 * .zip files will automatically be opened and processed. It's assumed that a .zip file contains only .csv file.
	 *
	 * @param outputDir A directory to store output phrase files.
	 * @param mappers A list of {@link PhraseMapper} objects.
	 * @throws IOException if any I/O errors occurs.
	 */
	public static PhraseSource process(PhraseSource input, Path outputDir, Collection<PhraseMapper> mappers,
	                                   PreprocessingOptions options) throws Exception {
		requireNonNull(input);
		requireNonNull(outputDir);
		requireNonNull(mappers);
		requireNonNull(options);

		// make a copy of the procession options
		options = new PreprocessingOptions(options);
		long start = System.currentTimeMillis();

		Util.createEmptyDirectory(outputDir);

		PhraseMapper[] mapperArray = mappers.toArray(new PhraseMapper[0]);
		MapperStats[] stats = options.verbose ? createStats(mapperArray) : null;

		if (options.mergeDuplicates) {
			Path tmp = outputDir.resolve("tmp");
			Util.createEmptyDirectory(tmp);

			// split all phrases by hash into different buckets such that duplicates are in
			// the same bucket
			try (SplitterCsvWriter writer = new SplitterCsvWriter(tmp, 1024)) {
				System.out.println("Applying mappers.");
				processAllFiles(options, input, file -> {
					try (PhraseReader reader = file.createReader()) {
						applyMappers(reader, writer, mapperArray, stats);
					}
				});
			}

			// use NetspeakCsvReader to read the output of SplitterNetspeakCsvWriter
			SimplePhraseSource tmpSource = new SimplePhraseSource(tmp);
			tmpSource.setReaderFactory(SimpleCsvReader::new);

			// delete temp files
			options.setDeleteSource(DeleteMode.PROGRESSIVE);

			// use a simple HashMap to merge the duplicates
			System.out.println("Merging phrases");
			AtomicLong totalPhrasesCount = new AtomicLong(0);
			AtomicLong totalDuplicatesCount = new AtomicLong(0);

			processAllFiles(options, tmpSource, file -> {
				Map<String, Long> map = new HashMap<>();
				try (PhraseReader reader = file.createReader()) {
					long phrases = 0;
					AtomicLong dups = new AtomicLong(0);
					PhraseFrequencyPair pair;
					while ((pair = reader.nextPair()) != null) {
						phrases++;
						map.merge(pair.phrase, pair.frequency, (a, b) -> {
							dups.incrementAndGet();
							return a + b;
						});
					}
					totalPhrasesCount.addAndGet(phrases - dups.get());
					totalDuplicatesCount.addAndGet(dups.get());
				}

				// write map
				Path out = outputDir.resolve(file.getPath().getFileName());
				try (SimpleCsvWriter writer = new SimpleCsvWriter(Files.newBufferedWriter(out, UTF_8))) {
					for (Entry<String, Long> entry : map.entrySet()) {
						writer.write(entry.getKey(), entry.getValue());
					}
				}
			});

			double percentage = Math
					.round(100. * 10. * totalDuplicatesCount.doubleValue() / totalPhrasesCount.doubleValue()) / 10.;
			System.out.println("Total of " + totalPhrasesCount + " phrases with " + totalDuplicatesCount + " ("
					+ percentage + "%) duplicates merged.");

			// clean up
			System.out.println("Deleting temporary directory");
			Files.delete(tmp);
		} else {

			System.out.println("Applying mappers.");
			processAllFiles(options, input, file -> {
				String outFileName = file.getPath().getFileName().toString().replaceFirst("(?i).csv[^\\\\/]*", "")
						+ ".csv";
				Path out = outputDir.resolve(Paths.get(outFileName));
				try (PhraseReader reader = file.createReader();
				     SimpleCsvWriter writer = new SimpleCsvWriter(Files.newBufferedWriter(out, UTF_8))) {
					applyMappers(reader, writer, mapperArray, stats);
				}
			});
		}

		printStats(stats);

		long end = System.currentTimeMillis();
		System.out.println("Took " + readableDuration(Duration.ofMillis(end - start)));
		System.out.println("Done.");

		return new SimplePhraseSource(outputDir);
	}

	/**
	 * This will iterate over all phases just as {@link #process(PhraseSource, Path, Collection, PreprocessingOptions)}
	 * would but without changing the file system.
	 * <p>
	 * All mappers can be thought of as consumers.
	 *
	 * @param mappers A list of {@link PhraseMapper} objects.
	 * @throws IOException if any I/O errors occurs.
	 */
	public static void iterate(PhraseSource input, Collection<PhraseMapper> mappers, PreprocessingOptions options)
			throws Exception {
		requireNonNull(input);
		requireNonNull(mappers);
		requireNonNull(options);

		// make a copy of the procession options
		options = new PreprocessingOptions(options);
		options.setDeleteSource(DeleteMode.NONE);
		long start = System.currentTimeMillis();

		System.out.println("Applying mappers.");
		PhraseMapper[] mapperArray = mappers.toArray(new PhraseMapper[0]);
		MapperStats[] stats = options.verbose ? createStats(mapperArray) : null;
		processAllFiles(options, input, file -> {
			try (PhraseReader reader = file.createReader()) {
				applyMappers(reader, null, mapperArray, stats);
			}
		});

		printStats(stats);

		long end = System.currentTimeMillis();
		System.out.println("Took " + readableDuration(Duration.ofMillis(end - start)));
		System.out.println("Done.");
	}

	private static void processAllFiles(PreprocessingOptions options, PhraseSource input, ProcessAllConsumer consumer)
			throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(options.parallelDegree);
		DeleteMode deleteSource = options.deleteSource;
		try {
			List<Future<Path>> futures = new ArrayList<>();
			int i = 0;
			Collection<PhraseSource.File> files = input.getFiles();
			for (final PhraseSource.File file : files) {
				int currentIndex = ++i;
				futures.add(executor.submit((ThrowsRunnable) () -> {
					int percent = currentIndex * 100 / files.size();
					String prefix = "[" + new Date() + "][" + percent + "% " + currentIndex + "/" + files.size() + "] ";
					System.out.println(prefix + "Preprocessing " + file);

					consumer.accept(file);

					if (deleteSource == DeleteMode.PROGRESSIVE) {
						Files.delete(file.getPath());
					}
				}, file.getPath()));
			}
			Util.getAll(futures); // wait for all tasks to complete

			if (deleteSource == DeleteMode.ATOMIC) {
				for (final PhraseSource.File file : files) {
					Files.delete(file.getPath());
				}
			}
		} finally {
			executor.shutdown();
			executor.awaitTermination(100, DAYS);
		}
	}

	@FunctionalInterface
	private interface ProcessAllConsumer {

		void accept(PhraseSource.File file) throws Exception;

	}

	private static void applyMappers(PhraseReader reader, PhraseWriter writer, PhraseMapper[] mappers,
	                                 MapperStats[] stats) throws Exception {
		PhraseFrequencyPair pair;
		while ((pair = reader.nextPair()) != null) {
			String newPhrase = mapAll(pair.phrase, pair.frequency, mappers, stats);
			if (newPhrase != null && writer != null) {
				writer.write(newPhrase, pair.frequency);
			}
		}
	}

	private static String mapAll(String phrase, long frequency, PhraseMapper[] mappers, MapperStats[] stats) {
		if (stats == null) {
			for (PhraseMapper mapper : mappers) {
				if (phrase == null || phrase.isEmpty())
					return null;
				phrase = mapper.map(phrase, frequency);
			}
			return phrase == null || phrase.isEmpty() ? null : phrase;
		} else {
			return mapAllWithStats(phrase, frequency, mappers, stats);
		}
	}

	private static String mapAllWithStats(String phrase, long frequency, PhraseMapper[] mappers, MapperStats[] stats) {
		if (phrase == null || phrase.isEmpty())
			return null;

		for (int i = 0; i < mappers.length; i++) {
			long start = System.nanoTime();
			String newPhrase = mappers[i].map(phrase, frequency);
			long time = System.nanoTime() - start;
			MapperStats s = stats[i];
			s.phrasesTotal.accumulate(1);
			s.runTime.accumulate(time);

			if (newPhrase == null || newPhrase.isEmpty()) {
				s.phrasesRemoved.accumulate(1);
				return null;
			} else {
				if (phrase.contentEquals(newPhrase)) {
					s.phrasesLeftUnchanged.accumulate(1);
				} else {
					s.phrasesChanged.accumulate(1);
					phrase = newPhrase;
				}
			}
		}
		return phrase;
	}

	private static MapperStats[] createStats(PhraseMapper[] mappers) {
		MapperStats[] stats = new MapperStats[mappers.length];
		for (int i = 0; i < mappers.length; i++) {
			stats[i] = new MapperStats(mappers[i]);
		}
		return stats;
	}

	private static void printStats(MapperStats[] stats) {
		if (stats == null)
			return;

		System.out.println();
		for (MapperStats s : stats) {
			long total = s.phrasesTotal.get();
			long changed = s.phrasesChanged.get();
			long kept = s.phrasesLeftUnchanged.get();
			long removed = s.phrasesRemoved.get();
			double runTime = s.runTime.get();

			System.out.println("Mapper: " + s.mapper.getName());
			System.out.println("  total  : " + padStart(total, 12));
			if (total > 0) {
				double t = total;
				System.out.println("  removed: " + padStart(removed, 12) + " (" + percent(removed / t, 2) + ")");
				System.out.println("  changed: " + padStart(changed, 12) + " (" + percent(changed / t, 2) + ")");
				System.out.println("  kept   : " + padStart(kept, 12) + " (" + percent(kept / t, 2) + ")");
				System.out.println("  time/phrase: " + round(runTime / total, 2) + "ns/p");
			}
		}
		System.out.println();
	}

	private static String padStart(Object o, int length) {
		String s = String.valueOf(o);

		if (s.length() >= length)
			return s;

		char[] spaces = new char[length - s.length()];
		for (int i = 0; i < spaces.length; i++) {
			spaces[i] = ' ';
		}

		return new String(spaces) + s;
	}

	private static String percent(double value, int precision) {
		return round(value * 100, precision) + "%";
	}

	private static String round(double value, int precision) {
		return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).toString();
	}

	private static String readableDuration(Duration duration) {
		return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
	}

	private static class MapperStats {

		public final PhraseMapper mapper;
		public final LongAccumulator phrasesTotal = new LongAccumulator(Long::sum, 0);
		public final LongAccumulator phrasesRemoved = new LongAccumulator(Long::sum, 0);
		public final LongAccumulator phrasesChanged = new LongAccumulator(Long::sum, 0);
		public final LongAccumulator phrasesLeftUnchanged = new LongAccumulator(Long::sum, 0);
		/**
		 * The total run time of the mapper in ns.
		 */
		public final LongAccumulator runTime = new LongAccumulator(Long::sum, 0);

		public MapperStats(PhraseMapper mapper) {
			this.mapper = mapper;
		}

	}

}
