package org.netspeak.usage;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;

import org.netspeak.Configuration;
import org.netspeak.Netspeak;
import org.netspeak.NetspeakUtil;
import org.netspeak.generated.NetspeakMessages.Request;
import org.netspeak.generated.NetspeakMessages.Response;

/**
 * Runs an interactive prompt to search Netspeak via command line.
 */
public class NetspeakTerminal {

	public static void main(String[] args) throws Exception {

		// ---------------------------------------------------------------------
		// CONFIGURATION
		// ---------------------------------------------------------------------
		Configuration config = new Configuration();
		config.put(Configuration.PATH_TO_HOME, "/media/michael/Volume/data-in-production/netspeak/netspeak3-web-en");
		config.put(Configuration.CACHE_CAPACITY, "10000");

		// ---------------------------------------------------------------------
		// START NETSPEAK
		// ---------------------------------------------------------------------
		Netspeak netspeak = new Netspeak(config);

		// ---------------------------------------------------------------------
		// TERMINAL INTERACTION
		// ---------------------------------------------------------------------
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));

		try (final Scanner scanner = new Scanner(System.in);) {
			Request.Builder rb = Request.newBuilder();
			while (true) {
				pw.print("\nEnter query (type 'q' to exit): ");
				pw.flush();
				String query = scanner.nextLine();
				if (query.equals("q"))
					break;
				long start = System.currentTimeMillis();
				Request request = rb.setQuery(query).build();
				Response response = netspeak.search(request);
				for (int i = 0; i != response.getPhraseCount(); ++i) {
					System.out.printf("%-5d%-15d%s\n", i, response.getPhrase(i).getFrequency(),
							NetspeakUtil.toString(response.getPhrase(i)));
				}
				System.out.println("Error code: " + response.getErrorCode());
				System.out.println("Error message: " + response.getErrorMessage());
				System.out.println("Tokenized query: " + String.join(" ", response.getQueryTokenList()));
				System.out.println("Parsed query: " + NetspeakUtil.toString(response.getQuery()));
				System.out.println("Time: " + (System.currentTimeMillis() - start));
				rb = request.toBuilder();
			}
		}
	}
}
