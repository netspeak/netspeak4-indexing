package org.netspeak.preprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Function;

import org.netspeak.io.PhraseFrequencyPair;
import org.netspeak.io.PhraseWriter;
import org.netspeak.io.SimpleCsvWriter;

/**
 * A phrase mapper that will create a vocabulary from all phrases it sees.
 * <p>
 * This mapper will not change any phrases.
 */
public class VocabularyExtractor implements PhraseMapper {

	private Map<String, LongAccumulator> vocabulary = new ConcurrentHashMap<>();
	private List<PhraseFrequencyPair> list;

	private List<PhraseFrequencyPair> getPairs() {
		if (list == null) {
			list = new ArrayList<>();
			vocabulary.forEach((phrase, counter) -> {
				list.add(new PhraseFrequencyPair(phrase, counter.get()));
			});
			vocabulary = null;

			list.sort((a, b) -> {
				if (a.frequency > b.frequency) {
					return -1;
				} else if (a.frequency < b.frequency) {
					return 1;
				}
				return a.phrase.compareTo(b.phrase);
			});
		}
		return list;
	}

	@Override
	public String map(String phrase, long frequency) {
		for (String word : phrase.split(" ")) {
			LongAccumulator counter = vocabulary.computeIfAbsent(word, key -> new LongAccumulator(Long::max, 0));
			counter.accumulate(frequency);
		}
		return phrase;
	}

	public void writePairs(PhraseWriter writer) throws Exception {
		for (PhraseFrequencyPair pair : getPairs()) {
			writer.write(pair);
		}
	}

	public void writePairs(Path file) throws Exception {
		writePairs(file, SimpleCsvWriter::new);
	}

	public void writePairs(Path file, Function<BufferedWriter, PhraseWriter> writerFactory) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE);
				PhraseWriter phraseWriter = writerFactory.apply(writer)) {
			writePairs(phraseWriter);
		}
	}

	public void writeVocabulary(BufferedWriter writer) throws IOException {
		String newLine = "\n";
		for (PhraseFrequencyPair pair : getPairs()) {
			writer.write(pair.phrase);
			writer.write(newLine);
		}
	}

	public void writeVocabulary(Path file) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
			writeVocabulary(writer);
		}
	}

	public Set<String> getVocabulary() {
		Set<String> set = new LinkedHashSet<>();
		for (PhraseFrequencyPair pair : getPairs()) {
			set.add(pair.phrase);
		}
		return set;
	}

}
