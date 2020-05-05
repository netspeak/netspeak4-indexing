package org.netspeak.usage;

import java.util.Map;

import org.netspeak.Configuration;
import org.netspeak.ErrorCode;
import org.netspeak.Netspeak;
import org.netspeak.NetspeakUtil;
import org.netspeak.generated.NetspeakMessages.Request;
import org.netspeak.generated.NetspeakMessages.Response;

import com.google.protobuf.InvalidProtocolBufferException;

public class NetspeakUsage {

	public static void main(String[] args) {

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
		// SEARCH NETSPEAK
		// ---------------------------------------------------------------------
		Request.Builder rb = Request.newBuilder();
		rb.setQuery("programming is *");
		// Advanced parameters (optional)
//		rb.setMaxPhraseCount(int);    // default: 100 (find at most X n-grams)
//		rb.setPhraseLengthMin(int);   // default: 1   (minimum n-gram length)
//		rb.setPhraseLengthMax(int);   // default: 5   (maximum n-gram length)

		Request request = rb.build();
		Response response = null;
		try {
			response = netspeak.search(request);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}

		// Tip: As you will see, there are no setter methods to prepare your
		// request object with a new query for your next search. But you can
		// and you should reuse your request object like that:
		// request = request.toBuilder().setQuery("be efficient and ?").build();

		// ---------------------------------------------------------------------
		// ERROR HANDLING
		// ---------------------------------------------------------------------
		// A Netspeak search will never throw any exceptions.
		// Errors are indicated by the response's error code.
		System.out.println("Error: " + response.getErrorCode());
		switch (ErrorCode.fromCode(response.getErrorCode())) {
		case NO_ERROR:
			// ...
			break;
		case INVALID_QUERY:
			// ...
			break;
		case SERVER_ERROR:
			// ...
			break;
		case UNKNOWN_ERROR:
			// ...
			break;
		}

		// You can also handle errors like this:
		// if (ErrorCode.cast(response.getErrorCode()) != ErrorCode.NO_ERROR)

		// ---------------------------------------------------------------------
		// READ RESPONSE
		// ---------------------------------------------------------------------
		// Returns the total frequency (100% basis) of the returned n-grams.
		// This is not the same value as the sum of all n-gram frequencies.
		System.out.println("Total frequency: " + response.getTotalFrequency());
		// Returns the tokenized query string produced by the query lexer.
		System.out.println("Tokenized query: " + String.join(" ", response.getQueryTokenList()));
		// Returns the parsed (valid) query produced by the query parser.
		System.out.println("Parsed query: " + NetspeakUtil.toString(response.getQuery()));
		// Returns the request object.
		System.out.println("Request was: " + response.getRequest());

		// Loop through the returned phrases
		for (int i = 0; i != response.getPhraseCount(); ++i) {
			System.out.printf("%-5d%-15d%s\n", i, response.getPhrase(i).getFrequency(),
					response.getPhrase(i).toString());
		}

		// You can also iterate like that:
		// for (Phrase phrase : response.getPhraseList()) {
		// System.out.println(phrase); // Complete phrase in JSON style
		// }

		// ---------------------------------------------------------------------
		// NETSPEAK PROPERTIES (Some interesting values)
		// ---------------------------------------------------------------------
		try {
			for (Map.Entry<String, String> entry : netspeak.getProperties().entrySet()) {
				System.out.println(entry);
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
}
