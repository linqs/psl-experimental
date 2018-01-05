/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2018 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.experimental.reasoner.general;

import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.reasoner.ExecutableReasoner;
import org.linqs.psl.reasoner.term.TermStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * A reasoner that calls into CVXPY for downstream solving.
 */
public class CVXPYReasoner extends ExecutableReasoner {
	public static final String MODEL_PATH =
			Paths.get(System.getProperty("java.io.tmpdir"), "psl_cvxpy_model.json").toString();

	public static final String RESULTS_PATH =
			Paths.get(System.getProperty("java.io.tmpdir"), "psl_cvxpy_results.json").toString();

	public CVXPYReasoner(ConfigBundle config) {
		super(config);

		this.executableInputPath = MODEL_PATH;
		this.executableOutputPath = RESULTS_PATH;
		this.args = new String[]{MODEL_PATH, RESULTS_PATH};
	}

	public CVXPYReasoner(ConfigBundle config, String executablePath) {
		super(config, executablePath, MODEL_PATH, RESULTS_PATH,	new String[]{MODEL_PATH, RESULTS_PATH});
	}

	@Override
	protected void writeModel(BufferedWriter modelWriter, TermStore termStore) throws IOException {
		if (!(termStore instanceof JSONSerialTermStore)) {
			throw new IllegalArgumentException("Requires a JSONSerialTermStore");
		}

		((JSONSerialTermStore)termStore).serialize(modelWriter);
	}

	@Override
	protected void readResults(BufferedReader resultsReader, TermStore termStore) throws IOException {
		if (!(termStore instanceof JSONSerialTermStore)) {
			throw new IllegalArgumentException("Requires a JSONSerialTermStore");
		}

		((JSONSerialTermStore)termStore).deserialize(resultsReader);
	}
}
