package org.netspeak.io;

import static java.util.Objects.requireNonNull;

public class PhraseFrequencyPair {

	public final String phrase;
	public final long frequency;

	/**
	 * Creates a new phrase frequency pair.
	 *
	 * @param phrase
	 * @param frequency
	 * @throws NullPointerException     if the given phrase is {@code null}.
	 * @throws IllegalArgumentException if the given frequency is {@code <= 0}.
	 */
	public PhraseFrequencyPair(final String phrase, final long frequency) {
		if (frequency <= 0) {
			throw new IllegalArgumentException();
		}
		this.phrase = requireNonNull(phrase);
		this.frequency = frequency;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PhraseFrequencyPair) {
			PhraseFrequencyPair other = (PhraseFrequencyPair) obj;
			return this.phrase.contentEquals(other.phrase) && this.frequency == other.frequency;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return phrase.hashCode() ^ (int) frequency ^ (int) (frequency >>> 32);
	}

}
