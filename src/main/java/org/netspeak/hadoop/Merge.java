package org.netspeak.hadoop;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.LongSumReducer;
import org.netspeak.lang.Agnostic;
import org.netspeak.lang.En;
import org.netspeak.lang.MapperConfig;
import org.netspeak.lang.SingleMapProcessor;
import org.netspeak.preprocessing.PhraseMapper;

public class Merge {
	private static final String CONFIG_LOWERCASE = "preprocessing.lowercase";
	private static final String CONFIG_MAX_N_GRAM = "preprocessing.max-n-gram";
	private static final String CONFIG_LANG = "preprocessing.lang";

	private static final String LANG_NONE = "none";
	private static final String LANG_EN = "en";
	private static final String LANG_DE = "de";

	public static class TokenizerMapper extends Mapper<Object, Text, Text, LongWritable> {

		private final Text phrase = new Text();
		private final LongWritable frequency = new LongWritable();

		private PhraseMapper[] mappers = new PhraseMapper[0];

		@Override
		public void setup(Context context) throws IOException, InterruptedException {
			final Configuration conf = context.getConfiguration();

			final MapperConfig config = new MapperConfig();
			config.lowercase = conf.getBoolean(CONFIG_LOWERCASE, false);
			config.maxNGram = conf.getInt(CONFIG_MAX_N_GRAM, Integer.MAX_VALUE);

			SingleMapProcessor processor;
			final String lang = conf.get(CONFIG_LANG, LANG_NONE).toLowerCase();
			switch (lang) {
			case LANG_NONE:
				processor = Agnostic.INSTANCE;
				break;
			case LANG_DE:
				throw new IllegalArgumentException("DE is not supported for Hadoop.");
			case LANG_EN:
				processor = En.INSTANCE;
				break;
			default:
				throw new IllegalArgumentException("Unknown language: " + lang);
			}

			try {
				mappers = processor.getMappers(config).toArray(new PhraseMapper[0]);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		private String map(String phrase, long frequency) {
			for (final PhraseMapper mapper : mappers) {
				phrase = mapper.map(phrase, frequency);
				if (phrase == null) {
					break;
				}
			}
			return phrase;
		}

		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			// format: <word> *( <spaces> <word> ) <tab> <frequency>
			final String line = value.toString().trim();
			if (line.isEmpty()) {
				// ignore line
				return;
			}

			final int tabIndex = line.indexOf('\t');
			if (tabIndex == -1) {
				throw new IOException("Invalid format: Unable to find tab character.");
			}

			final long freq = Long.parseLong(line.substring(tabIndex + 1));
			final String p = map(line.substring(0, tabIndex), freq);

			if (p == null) {
				return;
			}

			phrase.set(p);
			frequency.set(freq);

			context.write(phrase, frequency);
		}
	}

	public static void run(Collection<String> input, String outputDir, String lang, MapperConfig config)
			throws Exception {
		final Configuration conf = new Configuration();
		conf.set(CONFIG_LANG, lang);
		conf.setBoolean(CONFIG_LOWERCASE, config.lowercase);
		conf.setInt(CONFIG_MAX_N_GRAM, config.maxNGram);

		final Job job = Job.getInstance(conf, "Netspeak index preprocessing (" + lang + ")");
		job.setJarByClass(Merge.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(LongSumReducer.class);
		job.setReducerClass(LongSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		job.setNumReduceTasks(1000);

		FileInputFormat.setInputPaths(job, input.stream().map(Path::new).toArray(Path[]::new));
		FileOutputFormat.setOutputPath(job, new Path(outputDir));

		if (!job.waitForCompletion(true)) {
			throw new RuntimeException("Job failed.");
		}
	}

}
