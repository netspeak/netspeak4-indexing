package org.netspeak.preprocessing;

/**
 * An interface providing a {@link #map(String, long)} function that transforms
 * a given phrase. This interface can be used to apply certain string operations
 * on phrases, such as case conversion or removal. Filter instances can be
 * organized in some sort of collection to be applied one by one on the same
 * phrase.
 */
@FunctionalInterface
public interface PhraseMapper {

	/**
	 * Maps a given input {@code phrase} to some output phrase. The returned phrase
	 * may be {@code null} or the empty string in which case the phrase will be
	 * removed from the corpus.
	 * <p>
	 * The returned phrase is <b>not allowed</b> to contain tabs, line breaks,
	 * adjacent spaces, and leading or trailing spaces.
	 *
	 * @param phrase    The input phrase string. This is guaranteed to not be
	 *                  {@code null} and to not be the empty string.
	 * @param frequency The phrase frequency.
	 * @return The filtered phrase string.
	 */
	String map(String phrase, long frequency);

	/**
	 * The name of the PhraseMapper.
	 * <p>
	 * This name can be useful for diagnostics and will be used by
	 * {@link Preprocessing} when printing information about a {@link PhraseMapper}.
	 * By default this will be the name of the class of the mapper.
	 *
	 * @return
	 */
	default String getName() {
		return getClass().getName();
	}

	/**
	 * Returns a new {@link PhraseMapper} which behaves like the given
	 * {@link PhraseMapper} and with the name of the full name of the caller method.
	 *
	 * @param mapper
	 * @return
	 */
	static PhraseMapper rename(PhraseMapper mapper) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stack[2];
		return rename(caller.getClassName() + "." + caller.getMethodName(), mapper);
	}

	/**
	 * Returns a new {@link PhraseMapper} with the given name which behaves like the
	 * given {@link PhraseMapper}.
	 *
	 * @param name
	 * @param mapper
	 * @return
	 */
	static PhraseMapper rename(String name, PhraseMapper mapper) {
		return new PhraseMapper() {

			@Override
			public String map(String phrase, long frequency) {
				return mapper.map(phrase, frequency);
			}

			@Override
			public String getName() {
				return name;
			}
		};
	}

}
