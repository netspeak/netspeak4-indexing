package org.netspeak.lang;

import java.nio.file.Path;
import java.util.ArrayList;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.PreprocessingOptions.DeleteMode;
import org.netspeak.preprocessing.items.GermanHyphenationJoiner;
import org.netspeak.preprocessing.items.Operations;
import org.netspeak.preprocessing.mappers.ContractionMapper;
import org.netspeak.preprocessing.mappers.StandardMappers;

public class De implements Processor {

	public static final De INSTANCE = new De();

	private De() {
	}

	@Override
	public void process(Config config) throws Exception {
		final Path temp1 = config.newTempDir();
		final Path temp2 = config.newTempDir();

		Util.createEmptyDirectory(temp1);
		Util.createEmptyDirectory(temp2);

		try {

			final Pipeline pipeline = new Pipeline();

			pipeline.add(() -> {
				final Path output = temp1;

				final StandardMappers stdMappers = new StandardMappers();
				stdMappers.setSuperBlacklist(Util.readResourceWordList("/super-blacklist.txt"));
				stdMappers.setBlacklist(Util.readResourceWordList("/blacklist.txt"));
				stdMappers.setMaxNGram(config.maxNGram);
				stdMappers.setToLowerCase(config.lowercase);

				final ArrayList<PhraseMapper> mappers = new ArrayList<>(stdMappers.getMappers());

				mappers.add(new ContractionMapper(Util.readResourceWordList("/eng/contractions.txt")));

				return source -> Preprocessing.process(source, output, mappers, config.getPreprocessingOptions());
			});

			pipeline.add(() -> {
				final Path output = temp2;

				final PreprocessingOptions options = config.getPreprocessingOptions();
				options.setDeleteSource(DeleteMode.PROGRESSIVE); // delete files from temp

				return new GermanHyphenationJoiner(Util.readResourceWordList("/ger/stop-words.txt"), output, options);
			});

			pipeline.add(Operations.moveTo(config.output));

			pipeline.apply(config.source);

		} finally {
			Util.delete(temp1, true);
			Util.delete(temp2, true);
		}
	}

}
