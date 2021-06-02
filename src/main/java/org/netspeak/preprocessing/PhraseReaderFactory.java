package org.netspeak.preprocessing;

import java.io.BufferedReader;

import org.netspeak.io.PhraseReader;

/**
 * Given a {@link BufferedReader}, this will produce a {@link PhraseReader} that
 * parses the text content of the reader.
 *
 * @author micha
 */
@FunctionalInterface
public interface PhraseReaderFactory {

	PhraseReader createReader(BufferedReader reader) throws Exception;

}
