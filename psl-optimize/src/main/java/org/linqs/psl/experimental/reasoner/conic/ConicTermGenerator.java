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

import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.UnweightedGroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

public class ConicTermGenerator implements TermGenerator<ConicProgramProxy> {
	public void generateTerms(GroundRuleStore ruleStore, TermStore<ConicProgramProxy> termStore) {
		// We specifically need a ConicTermStore.
		if (!(termStore instanceof ConicTermStore)) {
			throw new IllegalArgumentException("Require a ConicTermStore.");
		}
		ConicTermStore conicTermStore = (ConicTermStore)termStore;

		for (GroundRule rule : ruleStore.getGroundRules()) {
			generateTerm(rule, conicTermStore);
		}
	}

	private void generateTerm(GroundRule rule, ConicTermStore termStore) {
		ConicProgramProxy proxy;
		if (rule instanceof WeightedGroundRule) {
			proxy = new FunctionConicProgramProxy(termStore, (WeightedGroundRule)rule);
		} else {
			proxy = new ConstraintConicProgramProxy(termStore, ((UnweightedGroundRule)rule).getConstraintDefinition(), rule);
		}

		termStore.add(rule, proxy);
	}

	public void updateWeights(GroundRuleStore ruleStore, TermStore<ConicProgramProxy> termStore) {
		// We specifically need a ConicTermStore.
		if (!(termStore instanceof ConicTermStore)) {
			throw new IllegalArgumentException("Require a ConicTermStore.");
		}
		ConicTermStore conicTermStore = (ConicTermStore)termStore;

		for (GroundRule groundRule : ruleStore.getGroundRules()) {
			if (groundRule instanceof WeightedGroundRule) {
				termStore.updateWeight((WeightedGroundRule)groundRule);
			}
		}
	}
}
