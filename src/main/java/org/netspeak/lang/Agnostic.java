package org.netspeak.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.netspeak.preprocessing.PhraseMapper;
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
public class Agnostic implements Processor, SingleMapProcessor {

	public static final Agnostic INSTANCE = new Agnostic();

	private Agnostic() {
	}

	@Override
	public Collection<PhraseMapper> getMappers(MapperConfig config) {
		final List<PhraseMapper> mappers = new ArrayList<>();
		if (config.maxNGram < Integer.MAX_VALUE) {
			mappers.add(PhraseMappers.maxNGram(config.maxNGram));
		}
		if (config.lowercase) {
			mappers.add(PhraseMappers.toLowerCase());
		}
		return mappers;
	}

}
