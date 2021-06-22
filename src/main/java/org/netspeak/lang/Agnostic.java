package org.netspeak.lang;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.items.Operations;
import org.netspeak.preprocessing.mappers.PhraseMappers;

/**
 * This processor implements language-independent operations.
 *
 * This will essentially just ensure that the given config options (e.g.
 * lower-case) are implemented.
 *
 * The purpose of this processor is the enable easy conversion between data set
 * formats and lower-casing of existing data sets.
 *
 * @author Michael
 */
public class Agnostic implements Processor {

	public static final Processor INSTANCE = new Agnostic();

	private Agnostic() {
	}

	@Override
	public void process(Config config) throws Exception {
		final String id = String.valueOf(Instant.now().toEpochMilli());
		final Path temp1 = config.temp.resolve("_temp-" + id);

		Util.delete(temp1, true);

		Util.createEmptyDirectory(temp1);

		try {

			final Pipeline pipeline = new Pipeline();

			pipeline.add(inputSource -> {
				final PreprocessingOptions options = new PreprocessingOptions();
				options.setParallelDegree(config.parallelDegree);
				options.setMergeDuplicates(config.mergeDuplicates);

				final List<PhraseMapper> mappers = new ArrayList<>();
				if (config.maxNGram < Integer.MAX_VALUE) {
					mappers.add(PhraseMappers.maxNGram(config.maxNGram));
				}
				if (config.lowercase) {
					mappers.add(PhraseMappers.toLowerCase());
				}

				return Preprocessing.process(inputSource, temp1, mappers, options);
			});

			pipeline.add(Operations.moveTo(config.output));

			pipeline.apply(config.source);

		} finally {
			Util.delete(temp1, true);
		}
	}

}
