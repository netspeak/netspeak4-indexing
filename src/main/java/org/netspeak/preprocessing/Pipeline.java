package org.netspeak.preprocessing;

import java.util.ArrayList;
import java.util.List;

import org.netspeak.Util.ThrowsSupplier;

public class Pipeline implements PipelineItem {

	private final List<PipelineItem> items = new ArrayList<>();

	public void add(PipelineItem item) {
		items.add(item);
	}

	public void add(ThrowsSupplier<PipelineItem> supplier) {
		items.add(supplier.get());
	}

	@Override
	public PhraseSource apply(PhraseSource source) throws Exception {
		for (PipelineItem item : items) {
			source = item.apply(source);
		}
		return source;
	}

}
