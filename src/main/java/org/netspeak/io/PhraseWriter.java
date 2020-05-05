package org.netspeak.io;

public interface PhraseWriter extends AutoCloseable {

	/**
	 * Writes the given phrase and frequency.
	 *
	 * @param phrase
	 * @param frequency
	 * @throws Exception
	 */
	void write(String phrase, long frequency) throws Exception;

	/**
	 * Writes the given phrase-frequency-pair.
	 *
	 * @param pair
	 * @throws Exception
	 */
	default void write(PhraseFrequencyPair pair) throws Exception {
		this.write(pair.phrase, pair.frequency);
	}

}
