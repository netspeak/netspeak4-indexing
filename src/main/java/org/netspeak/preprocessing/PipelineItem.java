package org.netspeak.preprocessing;

@FunctionalInterface
public interface PipelineItem {

	PhraseSource apply(PhraseSource source) throws Exception;

}
