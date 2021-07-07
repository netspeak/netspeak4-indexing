package org.netspeak.preprocessing.mappers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.netspeak.Util;
import org.netspeak.preprocessing.PhraseMapper;

public class StandardMappers {
	/**
	 * The maximum allowed number of words per phrase.
	 */
	int maxNGram = Integer.MAX_VALUE;
	/**
	 * Whether all phrases should be lower-cased.
	 */
	boolean toLowerCase = false;
	/**
	 * All phrases with at least one word which can be constructed from at most
	 * {@link #blacklistCombinations} many blacklisted word will be removed.
	 */
	Collection<String> blacklist = null;
	int blacklistCombinations = 4;
	/**
	 * @see PhraseMappers#superBlacklist(Iterable)
	 */
	Collection<String> superBlacklist = null;

	public void setBlacklist(Path blacklist) throws IOException {
		this.blacklist = Util.readWordList(blacklist);
	}

	public void setBlacklist(Collection<String> blacklist) {
		this.blacklist = blacklist;
	}

	public void setBlacklistCombinations(int blacklistCombinations) {
		this.blacklistCombinations = blacklistCombinations;
	}

	public void setSuperBlacklist(Path superBlacklist) throws IOException {
		this.superBlacklist = Util.readWordList(superBlacklist);
	}

	public void setSuperBlacklist(Collection<String> superBlacklist) {
		this.superBlacklist = superBlacklist;
	}

	public void setToLowerCase(boolean toLowerCase) {
		this.toLowerCase = toLowerCase;
	}

	public void setMaxNGram(int maxNGram) {
		this.maxNGram = maxNGram;
	}

	public Collection<PhraseMapper> getMappers() {
		final List<PhraseMapper> mappers = new ArrayList<>();

		// try to remove as much junk as possible
		// In this phase, phrases will only be removed and not altered.
		mappers.add(PhraseMappers.removeControlCharacters());
		if (superBlacklist != null) {
			mappers.add(PhraseMappers.superBlacklist(superBlacklist));
		}
		mappers.add(PhraseMappers.removeGoogleWebMarkers());
		mappers.add(PhraseMappers.removeHTMLEntities());
		mappers.add(PhraseMappers.removeURLsAndEmails());
		mappers.add(PhraseMappers.removeFileNames());

		// Normalization phase
		mappers.add(PhraseMappers.normalizeApostrophe());
		mappers.add(PhraseMappers.normalizeHyphens());
		mappers.add(PhraseMappers.explodeCommas());
		mappers.add(PhraseMappers.removeLeadingDoubleQuote());
		mappers.add(PhraseMappers.joinWordsWithLeadingApostrophe());

		if (blacklist != null) {
			mappers.add(PhraseMappers.blacklist(blacklist, blacklistCombinations));
		}
		if (maxNGram < Integer.MAX_VALUE) {
			mappers.add(PhraseMappers.maxNGram(maxNGram));
		}
		if (toLowerCase) {
			mappers.add(PhraseMappers.toLowerCase());
		}

		return mappers;
	}
}
