package org.netspeak.usage;

import org.netspeak.Util;
import org.netspeak.preprocessing.ContractionMapper;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.PhraseMappers;
import org.netspeak.preprocessing.PhraseSource;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.PreprocessingOptions;
import org.netspeak.preprocessing.SimplePhraseSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Demonstrates the usage of {@link Preprocessing}.
 */
public final class NetspeakBuilderUsage {

	public static void main(String[] args) throws Exception {

		Pipeline pipeline = new Pipeline();

		pipeline.add(source -> {
			Path output = Paths.get("C:\\Netspeak\\_out");

			PreprocessingOptions options = new PreprocessingOptions();
			options.setParallelDegree(8);
			options.setMergeDuplicates(true);

			Path conFile = Paths.get("D:\\netspeak\\contractions_eng.txt");
			ContractionMapper con = new ContractionMapper(conFile);

			PhraseMapper superBalcklist = PhraseMappers
					.superBlacklist(Util.readWordList(Paths.get("D:\\netspeak\\super_blacklist.txt")));

			return Preprocessing.process(source, output, Arrays.asList(superBalcklist, con), options);
		});

		SimplePhraseSource source1 = new SimplePhraseSource("C:\\Netspeak\\processed_corpora\\eng_ci_web+books");
		pipeline.apply(PhraseSource.combine(source1));
	}

}
