package org.netspeak.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;
import org.netspeak.preprocessing.mappers.ContractionMapper;
import org.netspeak.preprocessing.mappers.EnglishHyphenJoinMapper;
import org.netspeak.preprocessing.mappers.StandardMappers;

public class En implements Processor, SingleMapProcessor {

	public static final En INSTANCE = new En();

	private En() {
	}

	@Override
	public Collection<PhraseMapper> getMappers(MapperConfig config) throws IOException {
		final StandardMappers stdMappers = new StandardMappers();
		stdMappers.setSuperBlacklist(Util.readResourceWordList("/super-blacklist.txt"));
		stdMappers.setBlacklist(Util.readResourceWordList("/blacklist.txt"));
		stdMappers.setBlacklistCombinations(4);
		stdMappers.setMaxNGram(config.maxNGram);
		stdMappers.setToLowerCase(config.lowercase);

		final List<PhraseMapper> mappers = new ArrayList<>(stdMappers.getMappers());

		mappers.add(new EnglishHyphenJoinMapper());
		mappers.add(new ContractionMapper(Util.readResourceWordList("/eng/contractions.txt")));

		return mappers;
	}

}
