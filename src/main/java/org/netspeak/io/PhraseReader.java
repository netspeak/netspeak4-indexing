package org.netspeak.io;

/**
 * A interface for readers which return one phrase-frequency-pair at a time.
 *
 * @see GoogleBooksCsvReader
 * @see SimpleCsvReader
 *
 * @author Michael
 */
public interface PhraseReader extends AutoCloseable {

	/**
	 * Returns the next phrase-frequency-pair or {@code null} if no other pairs will
	 * be returned.
	 *
	 * @return
	 * @throws Exception
	 */
	PhraseFrequencyPair nextPair() throws Exception;

}
