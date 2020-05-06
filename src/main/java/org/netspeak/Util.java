package org.netspeak;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class Util {

	private Util() {
	}

	/**
	 * Deletes the given file or directory.
	 * <p>
	 * System links will not be followed. This will throw for non-empty directories. This operation will do nothing if
	 * the given path does not exist.
	 *
	 * @param dirOrFile
	 * @throws IOException
	 */
	public static void delete(Path dirOrFile) throws IOException {
		delete(dirOrFile, false);
	}

	/**
	 * Deletes the given file or directory (recursively).
	 * <p>
	 * System links will not be followed. This will throw for non-empty directories if not recursive. This operation
	 * will do nothing if the given path does not exist.
	 *
	 * @param dirOrFile
	 * @throws IOException
	 */
	public static void delete(Path dirOrFile, boolean recursive) throws IOException {
		if (!recursive) {
			Files.deleteIfExists(dirOrFile);
		} else {
			if (!Files.exists(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
				return;
			}
			Files.walkFileTree(dirOrFile, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static void createEmptyDirectory(Path dir) throws IOException {
		requireNonNull(dir);
		if (Files.isDirectory(dir)) {
			if (Files.newDirectoryStream(dir).iterator().hasNext()) {
				throw new AssertionError("Is not empty " + dir);
			}
		} else {
			Files.createDirectories(dir);
		}
	}

	public static <T> List<T> getAll(Iterable<Future<T>> futures) throws InterruptedException, ExecutionException {
		List<T> values = new ArrayList<>();
		for (Future<T> f : futures) {
			values.add(f.get());
		}
		return values;
	}

	/**
	 * Returns the
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Set<String> readWordList(Path path) throws IOException {
		try (FileInputStream fileIn = new FileInputStream(path.toFile());
		     Reader in = new InputStreamReader(fileIn, StandardCharsets.UTF_8)) {
			return readWordList(in);
		}
	}

	public static Set<String> readWordList(Reader in) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(in)) {
			Set<String> set = new LinkedHashSet<>();

			bufferedReader.lines().forEach(word -> {
				if (word == null || word.isEmpty())
					return;

				word = word.trim();
				if (!word.isEmpty()) {
					set.add(word);
				}
			});

			return set;
		}
	}

	public static Set<String> readWordList(String path) throws IOException {
		return readWordList(Paths.get(path));
	}

	public static Set<String> readResourceWordList(String name) throws IOException {
		try (InputStream input = Util.class.getResourceAsStream(name);
		     Reader in = new InputStreamReader(input, StandardCharsets.UTF_8)) {
			return readWordList(in);
		}
	}

	public static String toPhrase(String[] words) {
		if (words.length == 1)
			return words[0];

		StringBuilder sb = new StringBuilder();
		sb.append(words[0]);

		for (int i = 1; i < words.length; i++) {
			sb.append(' ');
			sb.append(words[i]);
		}

		return sb.toString();
	}


	/**
	 * Replaces all occurrences of the given pattern in the given string with the string returned by the replacer
	 * function.
	 *
	 * @param pattern
	 * @param string
	 * @param replacer
	 * @return
	 */
	public static String replaceAll(Pattern pattern, String string, Function<MatchResult, String> replacer) {
		Matcher matcher = pattern.matcher(string);

		requireNonNull(replacer);
		boolean result = matcher.find();
		if (result) {
			StringBuilder sb = new StringBuilder();
			int last = 0;
			do {
				sb.append(string, last, matcher.start());
				String replacement = replacer.apply(matcher);
				sb.append(replacement);
				last = matcher.end();
				result = matcher.find();
			} while (result);
			sb.append(string, last, string.length());
			return sb.toString();
		}
		return string;
	}


	public interface ThrowsRunnable extends Runnable {

		void runThrowing() throws Exception;

		@Override
		default void run() {
			try {
				runThrowing();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public interface ThrowsConsumer<T> extends Consumer<T> {

		void acceptThrowing(T t) throws Exception;

		@Override
		default void accept(T t) {
			try {
				acceptThrowing(t);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public interface ThrowsSupplier<T> extends Supplier<T> {

		T getThrowing() throws Exception;

		@Override
		default T get() {
			try {
				return getThrowing();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
