package org.netspeak.preprocessing;

import org.netspeak.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.util.Locale.ENGLISH;

public class ContractionMapper implements PhraseMapper {

	private final Pattern contractionPattern;
	private final Pattern incompleteContractionPattern;
	private final Map<String, Integer> knownContractionMap = new HashMap<>();
	private static final Pattern POSSESSIVE_S_PATTERN = Pattern.compile("s '(?= |\\z)", Pattern.CASE_INSENSITIVE);

	public ContractionMapper(Path file) throws IOException {
		this(Util.readWordList(file));
	}

	public ContractionMapper(Iterable<String> knownContractions) {
		StringBuilder pattern = new StringBuilder();
		Set<String> incompleteContractionSuffixes = new HashSet<>();
		Set<String> incompleteContractionPrefixes = new HashSet<>();

		for (String known : knownContractions) {
			// add know contractions ending with "n't" without ' to the map
			// we can't do this for contractions like "we'll" or "i'm" because of the false
			// positives
			if (known.endsWith("n't")) {
				for (String contraction : allCombinations(known)) {
					int index = contraction.indexOf('\'');
					knownContractionMap.put(contraction.replace("'", "").toLowerCase(ENGLISH), index);
				}
			}

			// add prefixes and suffixes to lists
			int apo = known.indexOf('\'');
			incompleteContractionPrefixes.addAll(allCombinations(known.substring(0, apo)));
			incompleteContractionSuffixes.addAll(allCombinations(known.substring(apo + 1)));

			// make it all non-capturing for better performance
			known = known.replace("\\((?!\\?)", "(?:");
			// replace the ' with all variations
			known = known.replace("'", "(?: '|' | ' | )");

			pattern.append(known);
			pattern.append('|');
		}
		pattern.append("[^\\s\\S]");

		// contractionPattern
		String finalPattern = "(?<= |\\A)(?:" + pattern.toString() + ")(?= |\\z)";
		// n't can be fixed with minimal context as it's the only contraction with both
		// prefix and suffix
		finalPattern += "|n(?: '|' | ' )t(?= |\\z)";
		// join possessive S
		finalPattern += "|(?: '|' | ' )s(?= |\\z)";

		contractionPattern = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);

		// incompleteContractionPattern
		incompleteContractionPrefixes.remove("");
		incompleteContractionSuffixes.remove("");

		StringBuilder incompletePattern = new StringBuilder();
		incompletePattern.append("(?:\\A| )(?:");
		incompletePattern.append(String.join("|", incompleteContractionPrefixes));
		incompletePattern.append(")(?: ?'\\z)");
		incompletePattern.append("|");
		incompletePattern.append("(?:\\A' ?)(?:");
		incompletePattern.append(String.join("|", incompleteContractionSuffixes));
		incompletePattern.append(")(?: |\\z)");

		incompleteContractionPattern = Pattern.compile(incompletePattern.toString(), Pattern.CASE_INSENSITIVE);
	}


	@Override
	public String map(String phrase, long frequency) {
		// phrases with incomplete contractions will be removed
		if (incompleteContractionPattern.matcher(phrase).find()) {
			return null;
		}


		phrase = Util.replaceAll(contractionPattern, phrase, match -> {
			String m = match.group();
			if (m.indexOf('\'') == -1) {
				// e.g. "don t"
				return m.replace(' ', '\'');
			} else {
				// e.g. "don' t" or "don 't" or "don ' t"
				return m.replace(" ", "");
			}
		});

		String[] words = phrase.split(" ");
		boolean changed = false;
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String lowercase = word.toLowerCase(ENGLISH);
			Integer ind = knownContractionMap.get(lowercase);
			if (ind != null) {
				int index = ind;
				words[i] = word.substring(0, index) + '\'' + word.substring(index);
				changed = true;
			}
		}

		phrase = changed ? Util.toPhrase(words) : phrase;

		phrase = POSSESSIVE_S_PATTERN.matcher(phrase).replaceAll("s'");

		return phrase;
	}

	private static List<String> allCombinations(String pattern) {
		List<Concatenation> alternatives = new ArrayList<>();
		parseAlternation(pattern, 0, alternatives::add);

		List<String> words = new ArrayList<>();
		for (Concatenation concat : alternatives) {
			List<StringBuilder> builders = new ArrayList<>();
			builders.add(new StringBuilder());
			addCombinations(builders, concat);
			builders.forEach(b -> words.add(b.toString()));
		}

		return words;
	}

	private static void addCombinations(List<StringBuilder> builders, Concatenation concat) {
		for (Element e : concat.getElements()) {
			if (e instanceof Literal) {
				String value = ((Literal) e).toString();
				builders.forEach(b -> b.append(value));
			} else {
				List<Concatenation> alternatives = ((Alternation) e).getConcatenations();
				List<StringBuilder> original = new ArrayList<>(builders);
				builders.clear();
				for (Concatenation alternative : alternatives) {
					List<StringBuilder> newBuilders = new ArrayList<>();
					original.forEach(b -> newBuilders.add(new StringBuilder(b)));
					addCombinations(newBuilders, alternative);
					builders.addAll(newBuilders);
				}
			}
		}
	}

	private static int parseAlternation(String pattern, final int startIndex, Consumer<Concatenation> consumeConcat) {
		int index = startIndex;

		List<Element> concat = new ArrayList<>();

		while (index < pattern.length()) {
			char c = pattern.charAt(index++);
			if (c == ')')
				break;
			if (c == '(') {
				List<Concatenation> alternatives = new ArrayList<>();
				index += parseAlternation(pattern, index, alternatives::add);
				if (alternatives.size() == 1) {
					concat.addAll(alternatives.get(0).getElements());
				} else {
					concat.add(new Alternation(alternatives));
				}
			} else if (c == '|') {
				consumeConcat.accept(new Concatenation(concat));
				concat = new ArrayList<>();
			} else {
				boolean added = false;
				if (!concat.isEmpty()) {
					Element last = concat.get(concat.size() - 1);
					if (last instanceof Literal) {
						((Literal) last).append(c);
						added = true;
					}
				}
				if (!added) {
					concat.add(new Literal(c));
				}
			}
		}

		consumeConcat.accept(new Concatenation(concat));

		return index - startIndex;
	}

	private interface Element {
	}

	private static class Concatenation {
		private final List<Element> elements;

		public Concatenation(List<Element> elements) {
			this.elements = elements;
		}

		public List<Element> getElements() {
			return elements;
		}

	}

	private static class Literal implements Element {
		private String value;

		public Literal(char value) {
			this.value = Character.toString(value);
		}

		public void append(char c) {
			this.value += c;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	private static class Alternation implements Element {
		private final List<Concatenation> concatenations;

		public Alternation(List<Concatenation> concatenations) {
			this.concatenations = concatenations;
		}

		public List<Concatenation> getConcatenations() {
			return concatenations;
		}
	}

}
