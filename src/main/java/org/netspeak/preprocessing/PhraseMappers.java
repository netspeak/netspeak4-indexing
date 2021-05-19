package org.netspeak.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Some common {@link PhraseMapper} to be used in {@link Preprocessing}.
 */
public final class PhraseMappers {

	/**
	 * Returns a new {@link PhraseMapper} which converts phrases to lower case.
	 * <p>
	 * Example: <code>"You'll make it"</code> becomes <code>"you'll make it"</code>
	 * </p>
	 *
	 * @return
	 */
	public static PhraseMapper toLowerCase() {
		return PhraseMapper.rename((phrase, frequency) -> phrase.toLowerCase());
	}

	/**
	 * Returns a new {@link PhraseMapper} which removes one leading double quote
	 * from a word.
	 * <p>
	 * Example: <code>"fo"o ""bar"</code> will become <code>fo"o "bar"</code> and
	 * <code>" foo</code> will stay <code>" foo</code>
	 * </p>
	 *
	 * @return
	 */
	public static PhraseMapper removeLeadingDoubleQuote() {
		return PhraseMapper.rename((phrase, frequency) -> LEADING_DOUBLE_QUOTE_PATTERN.matcher(phrase).replaceAll(""));
	}

	private static final Pattern LEADING_DOUBLE_QUOTE_PATTERN = Pattern
			.compile("(?:(?!\\G)|\\A)(?:\\A|(?<= ))\"(?=[^ ])");

	/**
	 * Returns a new {@link PhraseMapper} which joins two consecutive words within
	 * the phrase if the second word starts with an apostrophe.
	 * <p>
	 * Example: <code>"You 'll make it"</code> will become
	 * <code>"You'll make it"</code> and <code>"don 't"</code> will become
	 * <code>"don't"</code>
	 * </p>
	 *
	 * @return
	 */
	public static PhraseMapper joinWordsWithLeadingApostrophe() {
		return PhraseMapper.rename((phrase, frequency) -> phrase.replace(" '", "'"));
	}

	// https://en.wikipedia.org/wiki/Hyphen#Unicode
	private static final Pattern UNICODE_HYPHEN_APPTERN = Pattern.compile("[\u00ad\u2010\u2011]");

	/**
	 * Returns a new {@link PhraseMapper} that replaces all Unicode hyphen
	 * characters with the ASCII hyphen.
	 *
	 */
	public static PhraseMapper normalizeHyphens() {
		return PhraseMapper.rename((phrase, frequency) -> UNICODE_HYPHEN_APPTERN.matcher(phrase).replaceAll("-"));
	}

	// https://en.wikipedia.org/wiki/Apostrophe
	private static final Pattern UNICODE_APOSTROPHE_APPTERN = Pattern.compile("[\u2019\u02B9\u2032\u2035]");

	/**
	 * Returns a new {@link PhraseMapper} that replaces all Unicode apostrophe
	 * characters with the ASCII hyphen.
	 *
	 */
	public static PhraseMapper normalizeApostrophe() {
		return PhraseMapper.rename((phrase, frequency) -> UNICODE_APOSTROPHE_APPTERN.matcher(phrase).replaceAll("'"));
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases which contain at
	 * least one word that is contained in a given blacklist vocabulary.
	 *
	 * @param words
	 * @return
	 */
	public static PhraseMapper blacklist(final Collection<String> words) {
		return PhraseMapper.rename(blacklist(words, 1));
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases which contain at
	 * least one word that is an
	 * <a href="https://developer.mozilla.org/en-US/docs/Glossary/Entity">HTML
	 * entity</a>.
	 *
	 * @return
	 */
	public static PhraseMapper removeHTMLEntities() {
		return PhraseMapper.rename(filterByWords(w -> !(w.charAt(0) == '&' && w.charAt(w.length() - 1) == ';')));
	}

	/**
	 * Removes all control characters.
	 *
	 * See
	 * <a href="https://en.wikipedia.org/wiki/Control_character#In_Unicode">here</a>
	 * for more details.
	 *
	 * @return
	 */
	public static PhraseMapper removeControlCharacters() {
		return PhraseMapper.rename((phrase, freq) -> {
			final int l = phrase.length();
			for (int i = 0; i < l; i++) {
				final char c = phrase.charAt(i);
				if (c < ' ') // \x00 - \x1F
					return null;
				if (0x7F <= c && c <= 0x9F) // DEL, \x80 - \x9F
					return null;
			}
			return phrase;
		});
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases which contain at
	 * least one word that is contained in a given blacklist vocabulary.
	 * <p>
	 * Phrases which contains a word which can be constructed by concatenating
	 * {@code <= repeating} many words from the blacklist will also be removed. I.e.
	 * if {@code "} and {@code ?} are in the blacklist and {@code repeating} is 3,
	 * then {@code """}, {@code "?"}, {@code "?}, and {@code ??} will all be
	 * removed.
	 * <p>
	 * Please note that the blacklist will consume <b>{@code O(n ** repeat)}</b>
	 * many bytes of memory where {@code n} is the number of blacklist entries.
	 *
	 * @param words
	 * @return
	 */
	public static PhraseMapper blacklist(final Collection<String> words, int repeat) {
		HashSet<String> tempBlacklist = new HashSet<>(words);
		// just to be safe
		tempBlacklist.remove(null);
		tempBlacklist.remove("");

		if (repeat > 1) {
			tempBlacklist = new HashSet<>(getAllCombinations(tempBlacklist, repeat));
		}

		// thanks Java
		final Set<String> blacklist = tempBlacklist;

		return PhraseMapper.rename(filterByWords(w -> !blacklist.contains(w)));
	}

	private static List<String> getAllCombinations(Collection<String> words, int repeat) {
		final ArrayList<String> combinations = new ArrayList<>((int) Math.pow(words.size(), repeat));
		combinations.addAll(words);

		int start = 0;
		for (; repeat > 1; repeat--) {
			final int size = combinations.size();
			for (int i = start; i < size; i++) {
				for (final String word : words) {
					combinations.add(combinations.get(i) + word);
				}
			}
			start = size;
		}

		return combinations;
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases which contain at
	 * least one word that is not contained in an given whitelist vocabulary.
	 *
	 * @param words
	 * @return
	 */
	public static PhraseMapper whitelist(final Iterable<String> words) {
		final Set<String> whitelist = new HashSet<>();
		for (final String word : words)
			whitelist.add(word);

		return PhraseMapper.rename(filterByWords(whitelist::contains));
	}

	/**
	 * Returns a {@link PhraseMapper} which filters out all words for which the
	 * given predicate returns {@code false}.
	 *
	 * @param wordPredicate
	 * @return
	 */
	public static PhraseMapper filterByWords(final Predicate<String> wordPredicate) {
		return PhraseMapper.rename((phrase, frequency) -> {
			for (final String word : phrase.split(" ")) {
				if (!wordPredicate.test(word)) {
					return null;
				}
			}
			return phrase;
		});
	}

	/**
	 * Similar to {@link PhraseMappers#blacklist(Collection)} with the difference
	 * being that all phrase which contain any of the given strings anywhere will be
	 * removed.
	 * <p>
	 * E.g. A super blacklist with the string {@code "--"} will remove the phrase
	 * {@code "foo--bar"} while a normal blacklist will not.
	 *
	 * @param strings
	 * @return
	 */
	public static PhraseMapper superBlacklist(final Iterable<String> strings) {
		final StringMatcherNode matcher = StringMatcherNode.createRoot(strings);
		return PhraseMapper.rename((phrase, freq) -> {
			final int l = phrase.length();
			for (int i = 0; i < l; i++) {
				if (matcher.matches(phrase, i)) {
					return null;
				}
			}
			return phrase;
		});
	}

	private static class StringMatcherNode {

		private static final StringMatcherNode ACCEPT = new StringMatcherNode(true);

		private final StringMatcherNode[] next;

		private StringMatcherNode(boolean accept) {
			next = accept ? null : new StringMatcherNode[65536];
		}

		public boolean matches(String s, int index) {
			if (this == ACCEPT)
				return true;

			StringMatcherNode node = this;
			final int length = s.length();
			for (int i = index; i < length; i++) {
				if (node == ACCEPT)
					return true;

				final int c = s.charAt(i);
				node = node.next[c];
				if (node == null)
					return false;
			}
			return node == ACCEPT;
		}

		public static StringMatcherNode createRoot(final Iterable<String> words) {
			final StringMatcherNode root = new StringMatcherNode(false);

			for (final String word : words) {
				final int length = word.length();
				if (length == 0)
					return ACCEPT;

				StringMatcherNode node = root;
				for (int i = 0; i < length; i++) {
					final int c = word.charAt(i);
					if (i + 1 == length) {
						node.next[c] = ACCEPT;
					} else {
						StringMatcherNode current = node.next[c];
						if (current == ACCEPT)
							break;
						if (current == null)
							current = node.next[c] = new StringMatcherNode(false);
						node = current;
					}
				}
			}

			return root;
		}

	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases whose frequency is
	 * less than a given minimum frequency.
	 *
	 * @return
	 */
	public static PhraseMapper removeIfFrequencyIsLessThan(final long minimumFrequency) {
		return PhraseMapper.rename((phrase, frequency) -> frequency < minimumFrequency ? null : phrase);
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases that contain at least
	 * one character that is not included in the Latin-1 character set (ISO/IEC
	 * 8859-1). The Latin-1 character set contains all characters with code points
	 * in the range [0, 255]. ASCII is a subset of Latin-1 that covers the range [0,
	 * 127]. Since Latin-1 characters are encoded in 8 bit they are full compatible
	 * with languages that use simple 1-byte character types such as C or C++. You
	 * need to apply this filter as long as the native Netspeak C++ implementation
	 * has no built-in Unicode support.
	 *
	 * @return
	 */
	public static PhraseMapper removeIfContainsNonLatin1Chars() {
		final int maxLatin1CodePoint = 255;

		return PhraseMapper.rename((phrase, frequency) -> {
			for (int i = 0; i != phrase.length(); ++i) {
				if (phrase.codePointAt(i) > maxLatin1CodePoint) {
					return null;
				}
			}
			return phrase;
		});
	}

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases that contain URLs or
	 * email addresses.
	 *
	 * @return
	 */
	public static PhraseMapper removeURLsAndEmails() {
		return PhraseMapper.rename((phrase, frequency) -> {
			final String lower = phrase.toLowerCase();

			// check for Email addresses
			if (EMAIL_PATTERN.matcher(lower).find())
				return null;
			// matches the URL pattern
			if (URL_PATTERN.matcher(lower).find())
				return null;

			return phrase;
		});
	}

	// Email addresses can be right about anything which contains an @.
	private static final Pattern EMAIL_PATTERN = Pattern.compile(".@.");
	private static final String ALL_COUNTRY_TLD = "a[cdefgilmoqrstuwxz]|b[abdefghijmnorstwyz]|c[acdfghiklmnoruvwxyz]|d[ejkmoz]|e[cegrstu]|f[ijkmor]|g[adefghilmnpqrstuwy]|h[kmnrtu]|i[delmnoqrst]|j[emop]|k[eghimnprwyz]|l[abcikrstuvy]|m[acdeghklmnopqrstuvwxyz]|n[acefgilopruz]|om|p[aefghklmnrstwy]|qa|r[eosuw]|s[abcdeghiklmnorstuvxyz]|t[cdfghjklmnortvwz]|u[agksyz]|v[aceginu]|w[fs]|y[et]|z[amw]";
	// some of the more common domains
	// https://w3techs.com/technologies/overview/top_level_domain/all
	private static final Pattern URL_PATTERN = Pattern
			.compile("www\\.|https?:|ftps?:|\\.(?:com|org|net|edu|gov|xyz|moe|club|online|pro|site|top|shop|info|biz|"
					+ ALL_COUNTRY_TLD + ")\\b");

	/**
	 * Returns a new {@link PhraseMapper} that removes phrases that contain URLs or
	 * email addresses.
	 *
	 * @return
	 */
	public static PhraseMapper removeFileNames() {
		return PhraseMapper.rename((phrase, frequency) -> {
			final String lower = phrase.toLowerCase();

			if (FILE_NAME_PATTERN.matcher(lower).find())
				return null;

			return phrase;
		});
	}

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile(
			"\\.(?:exe|dll|bin|msi|bat|com|jar|pkg|apk|ini|ai|ico|jpg|jpeg|png|gif|bmp|webp|tif|tag|ps|odp|pps|ppt|pptx|pdf|doc|docx|xml|csv|sql|zip|rar|tar|gz|7z|iso|webm|mov|mkv|mpg|mpeg|mp3|acc|ogg|wav|wmv|mid|midi|mp4|avi|vlc|html|htm|php|asp|aspx|js|css)\\b");

	/**
	 * This removes all phrases with additional markers in the Google web corpus.
	 * This includes: {@code <s>}, {@code <S>}, {@code </s>}, {@code </S>},
	 * {@code </unk>}, and {@code </UNK>}.
	 *
	 * @return
	 */
	public static PhraseMapper removeGoogleWebMarkers() {
		return PhraseMapper.rename(blacklist(Arrays.asList("<s>", "<S>", "</s>", "</S>", "<unk>", "<UNK>")));
	}

	/**
	 * This will make surrounding commas some words have its own word.
	 * <p>
	 *
	 * <pre>
	 * "foo," -&gt; "foo ,"
	 * ",foo,," -&gt; ", foo, ,"
	 * </pre>
	 *
	 * @return
	 */
	public static PhraseMapper splitSurroundingCommas() {
		return PhraseMapper.rename((phrase, freq) -> {
			final String[] words = phrase.split(" ");
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				final int l = word.length();
				if (l > 1 && (word.charAt(0) == ',' || word.charAt(l - 1) == ',')) {
					if (word.contentEquals(",,")) {
						words[i] = ", ,";
					} else {
						if (word.charAt(0) == ',') {
							word = ", " + word.substring(1);
						}
						if (word.charAt(l - 1) == ',') {
							word = word.substring(0, l - 1) + " ,";
						}
					}
				}
			}
			return String.join(" ", words);
		});
	}

	public static PhraseMapper explodeCommas() {
		return PhraseMapper.rename((phrase, freq) -> {
			if (phrase.indexOf(',') >= 0) {
				return normalizeSpaces(phrase.replace(",", " , "));
			}
			return phrase;
		});
	}

	/**
	 * This will remove all phrase which have more than {@code n} words.
	 *
	 * @param n The maximum number of words allowed per phrase.
	 * @return
	 */
	public static PhraseMapper maxNGram(int n) {
		return PhraseMapper.rename((phrase, freq) -> {
			int words = 1;
			final int l = phrase.length();
			for (int i = 0; i < l; i++) {
				if (phrase.charAt(i) == ' ')
					words++;
			}
			return words > n ? null : phrase;
		});
	}

	private static final Pattern SPACES_PATTERN = Pattern.compile("\\s{2,}");

	private static String normalizeSpaces(String str) {
		return SPACES_PATTERN.matcher(str).replaceAll(" ").trim();
	}

}
