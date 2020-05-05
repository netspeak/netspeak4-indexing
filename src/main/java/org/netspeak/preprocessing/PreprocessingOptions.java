package org.netspeak.preprocessing;

import static java.util.Objects.requireNonNull;

public class PreprocessingOptions {
	int parallelDegree = 1;
	boolean mergeDuplicates = false;
	DeleteMode deleteSource = DeleteMode.NONE;
	boolean verbose = false;

	public PreprocessingOptions() {
	}

	public PreprocessingOptions(PreprocessingOptions toCopy) {
		parallelDegree = toCopy.parallelDegree;
		mergeDuplicates = toCopy.mergeDuplicates;
		deleteSource = toCopy.deleteSource;
		verbose = toCopy.verbose;
	}

	/**
	 * Sets the maximum number of concurrently processed files.
	 * <p>
	 * This defaults {@code 1} meaning that files will processed in a single thread.
	 *
	 * @param parallelDegree
	 */
	public void setParallelDegree(int parallelDegree) {
		this.parallelDegree = parallelDegree;
	}

	/**
	 * Sets whether to merge duplicate phrases between and within files.
	 * <p>
	 * This option is necessary if your phrases contain duplicates.
	 * <p>
	 * This defaults to {@code false}.
	 *
	 * @param mergeDuplicates
	 */
	public void setMergeDuplicates(boolean mergeDuplicates) {
		this.mergeDuplicates = mergeDuplicates;
	}

	/**
	 * Sets whether the source files will be deleted after they were read.
	 * <p>
	 * This option is useful to automatically remove temporary files.
	 * <p>
	 * This defaults to {@link DeleteMode#NONE}.
	 *
	 * @param deleteSource
	 */
	public void setDeleteSource(DeleteMode deleteSource) {
		this.deleteSource = requireNonNull(deleteSource);
	}

	public enum DeleteMode {
		/**
		 * No files will be deleted.
		 */
		NONE,
		/**
		 * All files will be deleted at once after all files have been read.
		 */
		ATOMIC,
		/**
		 * Files will be deleted as soon as possible.
		 */
		PROGRESSIVE
	}

	/**
	 * Sets whether additional information about the preprocessing step should be
	 * logged in the console.
	 * <p>
	 * Note: Enabling this might make the preprocessing slower.
	 * <p>
	 * This defaults to {code false}.
	 *
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

}
