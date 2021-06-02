package org.netspeak.lang;

/**
 * A processor implements language- or index-specific transformations on a given
 * data set.
 *
 * Processors should be pure functions in respect to the state of the program.
 * Processors should only modify the file system in the given output and
 * temporary directories. The contents of the temporary directories has to be
 * cleaned up the processor.
 *
 * @author Michael
 */
@FunctionalInterface
public interface Processor {

	void process(Config config) throws Exception;

}
