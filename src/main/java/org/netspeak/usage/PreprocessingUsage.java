package org.netspeak.usage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.netspeak.Util;
import org.netspeak.Util.ThrowsRunnable;
import org.netspeak.preprocessing.ContractionMapper;
import org.netspeak.preprocessing.HyphenationJoiner;
import org.netspeak.preprocessing.Operations;
import org.netspeak.preprocessing.Operations.StandardOperationsOptions;
import org.netspeak.preprocessing.PhraseMappers;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.PreprocessingOptions.DeleteMode;
import org.netspeak.preprocessing.SimplePhraseSource;

public class PreprocessingUsage {

	/*
	 * You have to specify two temporary directories.
	 *
	 * Ideally, these should be your fastest storage capable of holding the whole
	 * data set of a pipeline. This means you can read the data from a HDD, process
	 * it on an SSD.
	 */

	static Path temp1 = Paths.get("path/to/temp1");
	static Path temp2 = Paths.get("path/to/temp2");

	public static void main(String[] args) throws Exception {
		useTemp(() -> {

			PhraseSource german = new SimplePhraseSource("path/to/german/data");
			processGerman(german, Paths.get("out/german"));

		});
	}

	private static void useTemp(ThrowsRunnable runnable) throws Exception {
		// clear temporary directories before and after pre-processing
		Util.delete(temp1, true);
		Util.delete(temp2, true);
		try {
			runnable.runThrowing();
		} finally {
			Util.delete(temp1, true);
			Util.delete(temp2, true);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	static void processGerman(PhraseSource source, Path outDir) throws Exception {
		Pipeline pipeline = new Pipeline();

		pipeline.add(() -> {
			Path output = temp1;

			StandardOperationsOptions operationOptions = new StandardOperationsOptions();
			operationOptions.setSuperBlacklist(Util.readResourceWordList("super-blacklist.txt"));
			operationOptions.setBlacklist(Util.readResourceWordList("blacklist.txt"));
			operationOptions.setBlacklistCombinations(4);
			operationOptions.setMaxNGram(5);
			operationOptions.setToLowerCase(false);

			operationOptions.getAdditionalMappers()
					.add(new ContractionMapper(Util.readResourceWordList("eng/contractions.txt")));

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);

			return Operations.standardOperations(output, operationOptions, options);
		});

		pipeline.add(() -> {
			Path output = temp2;

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);
			options.setDeleteSource(DeleteMode.PROGRESSIVE); // delete files from temp

			HyphenationJoiner.German german = new HyphenationJoiner.German();
			german.setStopWordList(Util.readResourceWordList("ger/stop-words.txt"));

			return new HyphenationJoiner(german, output, options);
		});

		pipeline.add(Operations.moveTo(outDir));

		pipeline.apply(source);
	}

	static void processEnglish(PhraseSource source, Path outDir) throws Exception {
		Pipeline pipeline = new Pipeline();

		pipeline.add(() -> {
			Path output = temp1;

			StandardOperationsOptions operationOptions = new StandardOperationsOptions();
			operationOptions.setSuperBlacklist(Util.readResourceWordList("super-blacklist.txt"));
			operationOptions.setBlacklist(Util.readResourceWordList("blacklist.txt"));
			operationOptions.setBlacklistCombinations(4);
			operationOptions.setMaxNGram(5);
			operationOptions.setToLowerCase(false);

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);

			return Operations.standardOperations(output, operationOptions, options);
		});

		pipeline.add(() -> {
			Path output = temp2;

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);
			options.setDeleteSource(DeleteMode.PROGRESSIVE); // delete files from temp

			HyphenationJoiner.English english = new HyphenationJoiner.English();

			return new HyphenationJoiner(english, output, options);
		});

		pipeline.add(Operations.moveTo(outDir));

		pipeline.apply(source);
	}

	static void toLowerCase(PhraseSource source, Path outDir) throws Exception {
		Pipeline pipeline = new Pipeline();

		pipeline.add(inputSource -> {
			Path output = temp2;

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);

			return Preprocessing.process(inputSource, output, Arrays.asList(PhraseMappers.toLowerCase()), options);
		});

		pipeline.add(Operations.moveTo(outDir));

		pipeline.apply(source);
	}

}
