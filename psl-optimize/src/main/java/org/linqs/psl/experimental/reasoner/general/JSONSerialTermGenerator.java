/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2015 The Regents of the University of California
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

// TODO(eriq):
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.UnweightedGroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.arithmetic.AbstractGroundArithmeticRule;
import org.linqs.psl.model.rule.logical.AbstractGroundLogicalRule;
import org.linqs.psl.model.rule.logical.UnweightedGroundLogicalRule;
import org.linqs.psl.model.rule.logical.WeightedGroundLogicalRule;
import org.linqs.psl.reasoner.function.AtomFunctionVariable;
import org.linqs.psl.reasoner.function.ConstantNumber;
import org.linqs.psl.reasoner.function.ConstraintTerm;
import org.linqs.psl.reasoner.function.FunctionSingleton;
import org.linqs.psl.reasoner.function.FunctionSum;
import org.linqs.psl.reasoner.function.FunctionSummand;
import org.linqs.psl.reasoner.function.FunctionTerm;
import org.linqs.psl.reasoner.function.MaxFunction;
import org.linqs.psl.reasoner.function.PowerOfTwo;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

import java.util.ArrayList;
import java.util.List;

/**
 * A TermGenerator for terms meant to be serialized via JSON.
 */
public class JSONSerialTermGenerator implements TermGenerator<SimpleTerm> {
	@Override
	public void generateTerms(GroundRuleStore ruleStore, TermStore<SimpleTerm> termStore) {
		for (GroundRule groundRule : ruleStore.getGroundRules()) {
			SimpleTerm term = createTerm(groundRule);
			if (term.size() > 0) {
				termStore.add(groundRule, term);
			}
		}
	}

	@Override
	public void updateWeights(GroundRuleStore ruleStore, TermStore<SimpleTerm> termStore) {
		for (GroundRule groundRule : ruleStore.getGroundRules()) {
			if (groundRule instanceof WeightedGroundRule) {
				termStore.updateWeight((WeightedGroundRule)groundRule);
			}
		}
	}

	private SimpleTerm createTerm(GroundRule rule) {
		if (rule instanceof AbstractGroundLogicalRule) {
			return createLogicalTerm((AbstractGroundLogicalRule)rule);
		} else if (rule instanceof AbstractGroundArithmeticRule) {
			return createArithmeticTerm((AbstractGroundArithmeticRule)rule);
		}

		throw new RuntimeException("Unknown rule type: " + rule.getClass().getName());
	}

	private SimpleTerm createLogicalTerm(AbstractGroundLogicalRule rule) {
		boolean hard = (rule instanceof UnweightedGroundLogicalRule);
		boolean squared = !hard && (((WeightedGroundLogicalRule)rule).isSquared()) ;
		double weight = hard ? -1 : ((WeightedGroundLogicalRule)rule).getWeight();

		SimpleTerm term = new SimpleTerm(hard, squared, weight, 1.0);

		// Remember that logical rules store negated DNFs.

		for (GroundAtom atom : rule.getPositiveAtoms()) {
			if (atom instanceof ObservedAtom) {
				term.addConstant((ObservedAtom)atom, true);
			} else if (atom instanceof RandomVariableAtom) {
				term.addAtom((RandomVariableAtom)atom, true);
			}
		}

		for (GroundAtom atom : rule.getNegativeAtoms()) {
			if (atom instanceof ObservedAtom) {
				term.addConstant((ObservedAtom)atom, false);
			} else if (atom instanceof RandomVariableAtom) {
				term.addAtom((RandomVariableAtom)atom, false);
			}
		}

		// Add the final offset for positive terms.
		term.addConstant(-1.0 * rule.getPositiveAtoms().size());

		return term;
	}

	private SimpleTerm createArithmeticTerm(AbstractGroundArithmeticRule rule) {
		// TODO(eriq)
		throw new UnsupportedOperationException();
	}
}
