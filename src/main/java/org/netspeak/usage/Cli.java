package org.netspeak.usage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.netspeak.hadoop.Merge;
import org.netspeak.io.GoogleBooksCsvReader;
import org.netspeak.lang.Agnostic;
import org.netspeak.lang.Config;
import org.netspeak.lang.De;
import org.netspeak.lang.En;
import org.netspeak.lang.MapperConfig;
import org.netspeak.lang.Processor;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.SimplePhraseSourceFile;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "netspeak4-indexing", descriptionHeading = "%n", description = {
		"A CLI for the Netspeak 4 data preporcessing and indexing process." }, optionListHeading = "%nOptions:%n", mixinStandardHelpOptions = true)
public class Cli implements Runnable {

	@Option(names = { "-c", "--config" }, description = { "The path of a `.properties` config file.",
			"The keys of the file are interpreeted as long-name arguments passed to the CLI (unknown arguments will be ignored)."
					+ " This means that the CLI can be used by simply passing in a config file."
					+ " Note: If an argument is defined in the config file and passed explicitly to the CLI, the CLI argument will be used.",
			"To pass in multiple input paths, separate the paths using a semicolon (';')."
					+ " Leading and trailing spaces around paths will be removed." })
	Path config;

	@Option(names = { "-l", "--lang" }, description = { "The language of the data set(s) to process.",
			"Valid values:  ${COMPLETION-CANDIDATES}  (case insensitive)", "---",
			"NONE: This applies no language-specific transformations, it only enforces the passed options and merges duplicates."
					+ " This is useful if a data set needs to be lower-cased, decompressed, merged, or all three.",
			"EN: In addition to the passed options, it will also apply transformations specific to the English language.",
			"DE: In addition to the passed options, it will also apply transformations specific to the German language.",
			"---" })
	Lang lang;

	@Option(names = { "-i", "--input" }, type = String.class, arity = "1..*", description = {
			"A list of input directories.", "The file and directory formats will be automatically detected.",
			"The given files will not be modified." })
	List<String> input;
	@Option(names = { "-o", "--output" }, description = { "The output path of the preprocessing step.",
			"The given directory has to be either empty or not exist." })
	String output;
	@Option(names = { "-t", "--temp" }, description = { "A temporary path used to store temporary files.",
			"This path should point to a fast SSD."
					+ " Most processing and IO-heavy operations will be done in the temp directories."
					+ " All temporary files will be deleted during or at the end of execution.",
			"This option will be ignored when run with Hadoop." })
	String temp;

	@Option(names = { "--lowercase" }, description = { "Whether the whole data set will be lowercased." })
	Boolean lowercase;
	@Option(names = { "--max-n-gram" }, description = {
			"The maximum number of words an n-gram is allowed to contain. All n-grams with more words will be removed.",
			"By default, no n-grams will be removed based on length." })
	Integer maxNGram;
	@Option(names = { "--parallel" }, description = { "The number of concurrent threads doing the processing.",
			"If set to 0 or any negative number, all available processing cores will be used.",
			"By default, all available processor cores will be used." })
	Integer parallel;
	@Option(names = { "--merge" }, description = { "Whether duplicate phrases in the data set will be merged.",
			"Defaults to true." })
	Boolean merge;
	@Option(names = { "--hadoop" }, description = { "Whether to do the given operation on a Hadoop cluster.",
			"Defaults to false." })
	Boolean hadoop;

	private void readConfig() throws Throwable {
		if (config == null) {
			return;
		}

		final Properties props = new Properties();
		props.load(Files.newBufferedReader(config, UTF_8));

		String p;

		if (lang == null) {
			p = props.getProperty("lang");
			if (p != null) {
				lang = Lang.valueOf(p.toUpperCase());
			}
		}

		if (input == null) {
			p = props.getProperty("input");
			if (p != null) {
				input = Arrays.stream(p.split(";")).map(String::trim).filter(s -> !s.isEmpty())
						.collect(Collectors.toList());
			}
		}

		if (output == null) {
			p = props.getProperty("output");
			if (p != null) {
				output = p;
			}
		}

		if (temp == null) {
			p = props.getProperty("temp");
			if (p != null) {
				temp = p;
			}
		}

		if (lowercase == null) {
			p = props.getProperty("lowercase");
			if (p != null) {
				lowercase = Boolean.parseBoolean(p);
			}
		}

		if (maxNGram == null) {
			p = props.getProperty("max-n-gram");
			if (p != null) {
				maxNGram = Integer.parseInt(p);
			}
		}

		if (parallel == null) {
			p = props.getProperty("parallel");
			if (p != null) {
				parallel = Integer.parseInt(p);
			}
		}

		if (merge == null) {
			p = props.getProperty("merge");
			if (p != null) {
				merge = Boolean.parseBoolean(p);
			}
		}

	}

	private PhraseSource toPhraseSource(Path input) throws IOException {
		Path data = input.resolve("data");
		if (!Files.isDirectory(data) && Files.isDirectory(input.resolve("1gms"))) {
			data = input;
		}

		if (Files.isDirectory(data)) {
			// Google Web

			final Collection<PhraseSource.File> sourceFiles = new ArrayList<>();

			// 1gms is special
			final Path oneGrams = data.resolve("1gms");
			final List<String> vocabFiles = Arrays.asList("vocab_cs.gz", "vocab_cs.bz2");
			for (final String file : vocabFiles) {
				final Path path = oneGrams.resolve(file);
				if (Files.isRegularFile(path)) {
					sourceFiles.add(new SimplePhraseSourceFile(path));
					break;
				}
			}
			if (sourceFiles.isEmpty()) {
				throw new IOException("Unable to find 1-gram file.");
			}

			for (int n = 2;; n++) {
				final Path dir = data.resolve(n + "gms");
				if (!Files.isDirectory(dir)) {
					break;
				}

				// all files are of the name "<n>gm-0000.<ext>"
				final String prefix = n + "gm-";

				Files.list(dir).filter(Files::isRegularFile).filter(p -> p.getFileName().toString().startsWith(prefix))
						.map(SimplePhraseSourceFile::new).forEach(sourceFiles::add);
			}

			return PhraseSource.fromFiles(sourceFiles);
		}

		final List<Path> files = Files.list(input).filter(Files::isRegularFile).collect(Collectors.toList());
		if (files.isEmpty()) {
			throw new IOException("No files in directory " + input.toString());
		}

		if (files.stream().anyMatch(f -> f.getFileName().toString().startsWith("googlebooks-"))) {
			// Google Books
			return PhraseSource.fromFiles(files.stream()
					.map(p -> new SimplePhraseSourceFile(p, GoogleBooksCsvReader::new)).collect(Collectors.toList()));
		}

		// assume simple CSV format
		return PhraseSource.fromFiles(files.stream().map(SimplePhraseSourceFile::new).collect(Collectors.toList()));
	}

	private void runLocal() throws Throwable {
		final PhraseSource source = PhraseSource.combine(input.stream().map(p -> {
			try {
				return toPhraseSource(Paths.get(p));
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(toList()));

		final Config config = new Config(source, Paths.get(output));
		config.temp = Paths.get(temp);
		config.lowercase = lowercase == null ? false : lowercase;
		config.maxNGram = maxNGram == null ? Integer.MAX_VALUE : maxNGram;
		config.parallelDegree = parallel == null || parallel <= 0 ? Runtime.getRuntime().availableProcessors()
				: parallel;
		config.mergeDuplicates = merge == null ? true : merge;

		lang.processor.process(config);
	}

	private void runHadoop() throws Throwable {
		if (merge == false) {
			throw new IllegalArgumentException(
					"When running using Hadoop, duplicates will always be merged. This conflicts with the `merge=false` given.");
		}

		final MapperConfig config = new MapperConfig();
		config.lowercase = lowercase == null ? false : lowercase;
		config.maxNGram = maxNGram == null ? Integer.MAX_VALUE : maxNGram;

		Merge.run(input, output, lang.name(), config);
	}

	private void runWithExecption() throws Throwable {
		readConfig();

		if (input == null) {
			throw new IllegalArgumentException("--input option is not set by config file or argument.");
		}
		if (input.isEmpty()) {
			throw new IllegalArgumentException("You need to provide at least one input directory.");
		}
		if (output == null) {
			throw new IllegalArgumentException("--output option is not set by config file or argument.");
		}
		if (lang == null) {
			throw new IllegalArgumentException("--lang option is not set by config file or argument.");
		}

		if (hadoop == true) {
			runHadoop();
		} else {
			runLocal();
		}

		System.out.println("Done.");
	}

	@Override
	public void run() {
		try {
			runWithExecption();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private enum Lang {
		NONE(Agnostic.INSTANCE), EN(En.INSTANCE), DE(De.INSTANCE);

		public final Processor processor;

		Lang(Processor processor) {
			this.processor = processor;
		}
	}

	public static void main(String[] args) {
		final CommandLine cli = new CommandLine(new Cli());
		cli.setCaseInsensitiveEnumValuesAllowed(true);

		// Hint for devs: You can replace `args` with string arguments.
		// E.g. `"--input", "../baz", "-o=./out"`.
		final int exitCode = cli.execute(args);

		System.exit(exitCode);
	}

}
