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
package org.linqs.psl.experimental.reasoner.conic;

import org.linqs.psl.experimental.optimizer.conic.ConicProgramSolver;
import org.linqs.psl.experimental.optimizer.conic.program.ConicProgram;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.rule.UnweightedGroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.function.AtomFunctionVariable;
import org.linqs.psl.reasoner.term.TermStore;

import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Reasoner that uses a {@link ConicProgramSolver} to minimize the total weighted
 * incompatibility.
 */
public class ConicReasoner implements Reasoner {
	@Override
	public void optimize(TermStore termStore) {
		// We specifically need a ConicTermStore.
		if (!(termStore instanceof ConicTermStore)) {
			throw new IllegalArgumentException("Require a ConicTermStore.");
		}
		ConicTermStore conicTermStore = (ConicTermStore)termStore;

		conicTermStore.getSolver().solve();
		conicTermStore.updateAtoms();
	}

	@Override
	public void close() {
	}
}
