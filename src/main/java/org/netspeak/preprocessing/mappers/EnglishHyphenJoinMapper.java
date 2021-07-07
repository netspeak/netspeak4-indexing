package org.netspeak.preprocessing.mappers;

import java.util.regex.Pattern;

import org.netspeak.preprocessing.PhraseMapper;

public class EnglishHyphenJoinMapper implements PhraseMapper {

	@Override
	public String map(String phrase, long frequency) {
		if (phrase.indexOf("-") == -1)
			return phrase;

		// The first replacement done by this mapper is to replace " - " with "-".
		// This means that e.g. "foo - bar" will be mapped to "foo-bar".
		// Since the dataset will likely contain transitive sub-n-grams as well, all
		// phrases that start or end with the word "-" will be removed, e.g. "foo -".
		//
		// Another replacement is to join words like this: "foo- bar" -> "foo-bar".
		// This pattern is relatively common. The only exception here are phrases
		// like "pre- and post-war-period writing".

		if (phrase.startsWith("- ") || phrase.endsWith(" -")) {
			return null;
		}

		// e.g. "foo - bar" -> "foo-bar"
		phrase = joinThreeWords(phrase);

		// e.g. "foo- bar" -> "foo-bar"
		phrase = joinTwoWords(phrase);

		return phrase;
	}

	private String joinThreeWords(String phrase) {
		return phrase.replace(" - ", "-");
	}

	private static final Pattern WORD_JOIN_PATTERN = Pattern.compile("(?<=[a-z])- (?!and\\b|or\\b)(?=[a-z])",
			Pattern.CASE_INSENSITIVE);

	private String joinTwoWords(String phrase) {
		return WORD_JOIN_PATTERN.matcher(phrase).replaceAll("-");
	}

}
