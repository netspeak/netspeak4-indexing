package org.netspeak.lang;

import java.nio.file.Path;
import java.util.Collection;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.Pipeline;
import org.netspeak.preprocessing.Preprocessing;
import org.netspeak.preprocessing.items.Operations;

public interface SingleMapProcessor extends Processor {

	/**
	 * Returns a read-only collection of mappers.
	 *
	 * @param config
	 * @return
	 * @throws Exception
	 */
	Collection<PhraseMapper> getMappers(MapperConfig config) throws Exception;

	@Override
	default void process(Config config) throws Exception {
		final Path temp = config.newTempDir();

		Util.createEmptyDirectory(temp);

		try {

			final Pipeline pipeline = new Pipeline();

			pipeline.add(() -> {
				final Path output = temp;

				return source -> Preprocessing.process(source, output, getMappers(config),
						config.getPreprocessingOptions());
			});

			pipeline.add(Operations.moveTo(config.output));

			pipeline.apply(config.source);

		} finally {
			Util.delete(temp, true);
		}
	}

}
