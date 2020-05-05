package org.netspeak.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Test;

public class ContractionMapperTest {

	public List<String> getContractionPatterns() {
		List<String> patterns = new ArrayList<>();

		patterns.add("i'm");
		patterns.add("(he|she|it)'s");
		patterns.add("(you|we|they)'re");
		patterns.add("(i|you|he|she|it|we|they)'(d|ll|ve)");
		patterns.add("y'all");
		patterns.add("(have|has|had|do|does|did|is|are|ai|was|were|wo|would|ca|could|sha|must|need)n't");

		return patterns;
	}

	@Test
	public void contractionTest() {
		final ContractionMapper mapper = new ContractionMapper(getContractionPatterns());

		BiConsumer<String, String> test = (from, to) -> {
			String actual = mapper.map(from, 100);
			if (actual == to)
				return;
			if (to == null || actual == null || !to.contentEquals(actual)) {
				Assert.fail("\"" + from + "\" was expected to map to \"" + to + "\" but was actually mapped to \""
						+ actual + "\".");
			}
		};

		test.accept("Tom", "Tom");
		test.accept("Tom's bar", "Tom's bar");
		test.accept("Tom 's bar", "Tom's bar");
		test.accept("Tom ' s bar", "Tom's bar");
		test.accept("Tom' s bar", "Tom's bar");
		test.accept("Tom s bar", "Tom s bar"); // too little context, so leave it as is

		test.accept("Charls' phone", "Charls' phone");
		test.accept("Charls ' phone", "Charls' phone");
		test.accept("Charls '", "Charls'");
		test.accept("Charls 't", "Charls 't");

		test.accept("he's nice", "he's nice");
		test.accept("he' s nice", "he's nice");
		test.accept("he ' s nice", "he's nice");
		test.accept("he 's nice", "he's nice");
		test.accept("he s nice", "he's nice");

		test.accept("we'll do it", "we'll do it");
		test.accept("we 'll do it", "we'll do it");
		test.accept("we ' ll do it", "we'll do it");
		test.accept("we' ll do it", "we'll do it");
		test.accept("we ll do it", "we'll do it");
		test.accept("well do it", "well do it"); // well well well

		test.accept("dont", "don't");
		test.accept("don't", "don't");
		test.accept("don 't", "don't");
		test.accept("don ' t", "don't");
		test.accept("don' t", "don't");
		test.accept("don t", "don't");

		test.accept("DoNt", "DoN't");
		test.accept("DoN't", "DoN't");
		test.accept("DoN 't", "DoN't");
		test.accept("DoN ' t", "DoN't");
		test.accept("DoN' t", "DoN't");
		test.accept("DoN t", "DoN't");

		test.accept("I'm", "I'm");
		test.accept("I 'm", "I'm");
		test.accept("I ' m", "I'm");
		test.accept("I' m", "I'm");
		test.accept("I m", "I'm");

		test.accept("I might", "I might");

		test.accept("won", "won");
		test.accept("won'", null);
		test.accept("won '", null);
		test.accept("'t open", null);
		test.accept("' t open", null);
		test.accept("t open", "t open"); // could be real
	}

}
