package org.netspeak.lang;

import java.nio.file.Path;
import java.time.Instant;

import org.netspeak.Util;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.PreprocessingOptions.DeleteMode;
import org.netspeak.preprocessing.items.HyphenationJoiner;
import org.netspeak.preprocessing.items.Operations;
import org.netspeak.preprocessing.items.Operations.StandardOperationsOptions;
import org.netspeak.preprocessing.mappers.ContractionMapper;

public class De implements Processor {

	public static final Processor INSTANCE = new De();

	private De() {
	}

	@Override
	public void process(Config config) throws Exception {
		final String id = String.valueOf(Instant.now().toEpochMilli());
		final Path temp1 = config.temp.resolve("_temp1-" + id);
		final Path temp2 = config.temp.resolve("_temp2-" + id);

		Util.delete(temp1, true);
		Util.delete(temp2, true);

		Util.createEmptyDirectory(temp1);
		Util.createEmptyDirectory(temp2);

		try {

			final Pipeline pipeline = new Pipeline();

			pipeline.add(() -> {
				final Path output = temp1;

				final StandardOperationsOptions operationOptions = new StandardOperationsOptions();
				operationOptions.setSuperBlacklist(Util.readResourceWordList("/super-blacklist.txt"));
				operationOptions.setBlacklist(Util.readResourceWordList("/blacklist.txt"));
				operationOptions.setBlacklistCombinations(4);
				operationOptions.setMaxNGram(config.maxNGram);
				operationOptions.setToLowerCase(config.lowercase);

				operationOptions.getAdditionalMappers()
						.add(new ContractionMapper(Util.readResourceWordList("/eng/contractions.txt")));

				final PreprocessingOptions options = new PreprocessingOptions();
				options.setParallelDegree(config.parallelDegree);
				options.setMergeDuplicates(true);

				return Operations.standardOperations(output, operationOptions, options);
			});

			pipeline.add(() -> {
				final Path output = temp2;

				final PreprocessingOptions options = new PreprocessingOptions();
				options.setParallelDegree(config.parallelDegree);
				options.setMergeDuplicates(true);
				options.setDeleteSource(DeleteMode.PROGRESSIVE); // delete files from temp

				final HyphenationJoiner.German german = new HyphenationJoiner.German();
				german.setStopWordList(Util.readResourceWordList("/ger/stop-words.txt"));

				return new HyphenationJoiner(german, output, options);
			});

			pipeline.add(Operations.moveTo(config.output));

			pipeline.apply(config.source);

		} finally {
			Util.delete(temp1, true);
			Util.delete(temp2, true);
		}
	}

}
