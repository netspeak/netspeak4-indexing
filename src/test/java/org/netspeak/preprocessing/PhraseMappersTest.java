package org.netspeak.preprocessing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.netspeak.preprocessing.mappers.PhraseMappers;

public class PhraseMappersTest {

	private void phraseMapperTest(PhraseMapper mapper, Collection<String> unchanged, Collection<String> removed,
			Map<String, String> changed) {
		final String name = mapper.getName();

		if (unchanged != null) {
			for (final String expected : unchanged) {
				final String actual = mapper.map(expected, 100);
				assertEquals("Expected unchanged for " + name, expected, actual);
			}
		}

		if (removed != null) {
			for (final String expected : removed) {
				final String actual = mapper.map(expected, 100);
				assertEquals("Expected removed for " + name, null, actual);
			}
		}

		if (changed != null) {
			for (final Map.Entry<String, String> transform : changed.entrySet()) {
				final String actual = mapper.map(transform.getKey(), 100);
				assertEquals("Expected changed for " + name, transform.getValue(), actual);
			}
		}
	}

	@Test
	public void blacklist() {
		final Set<String> blacklistedWords = new HashSet<>();
		for (final String word : ". - ( ) \" '".split(" ")) {
			blacklistedWords.add(word);
		}

		final Collection<String> sharedUnchanged = new ArrayList<>();
		sharedUnchanged.add("foo bar");
		sharedUnchanged.add("foo-bar");
		sharedUnchanged.add("Dr.");

		final Collection<String> sharedRemoved = new ArrayList<>();
		sharedRemoved.add(".");
		sharedRemoved.add(".");
		sharedRemoved.add("(");
		sharedRemoved.add(")");
		sharedRemoved.add("-");
		sharedRemoved.add("foo -");
		sharedRemoved.add("- foo");
		sharedRemoved.add("- foo -");
		sharedRemoved.add("foo - bar");

		{
			final PhraseMapper mapper = PhraseMappers.blacklist(blacklistedWords);

			final Collection<String> unchanged = new ArrayList<>(sharedUnchanged);
			unchanged.add("()");

			final Collection<String> removed = new ArrayList<>(sharedRemoved);

			phraseMapperTest(mapper, unchanged, removed, null);
		}
		{
			final PhraseMapper mapper = PhraseMappers.blacklistRepeated(blacklistedWords);

			final Collection<String> unchanged = new ArrayList<>();
			unchanged.add("()()a");

			final Collection<String> removed = new ArrayList<>();
			removed.add("()()");
			removed.add("-.-.");
			removed.add("-.-.-.-.-.-.-.-.-.-");
			removed.add("-.-. foo");
			removed.add("foo -.-. foo");

			phraseMapperTest(mapper, unchanged, removed, null);
		}
	}

	@Test
	public void superBlacklist() {
		final Set<String> blacklistedWords = new HashSet<>();
		for (final String word : ". - ( ) \" '".split(" ")) {
			blacklistedWords.add(word);
		}

		final Collection<String> sharedUnchanged = new ArrayList<>();
		sharedUnchanged.add("foo bar");
		sharedUnchanged.add("foo-bar");
		sharedUnchanged.add("Dr.");

		final Collection<String> sharedRemoved = new ArrayList<>();
		sharedRemoved.add(".");
		sharedRemoved.add(".");
		sharedRemoved.add("(");
		sharedRemoved.add(")");
		sharedRemoved.add("-");
		sharedRemoved.add("foo -");
		sharedRemoved.add("- foo");
		sharedRemoved.add("- foo -");
		sharedRemoved.add("foo - bar");

		{
			final PhraseMapper mapper = PhraseMappers.superBlacklist(blacklistedWords);

			final Collection<String> unchanged = new ArrayList<>();
			unchanged.add("foo bar");

			final Collection<String> removed = new ArrayList<>();
			removed.add(".");
			removed.add(".");
			removed.add("(");
			removed.add(")");
			removed.add("-");
			removed.add("foo -");
			removed.add("- foo");
			removed.add("- foo -");
			removed.add("foo - bar");

			removed.add("foo-bar");
			removed.add("Dr.");

			phraseMapperTest(mapper, unchanged, removed, null);
		}
	}

}
