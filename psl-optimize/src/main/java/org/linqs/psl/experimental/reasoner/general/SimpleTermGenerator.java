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

import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.predicate.SpecialPredicate;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.arithmetic.AbstractGroundArithmeticRule;
import org.linqs.psl.model.rule.arithmetic.UnweightedGroundArithmeticRule;
import org.linqs.psl.model.rule.arithmetic.WeightedGroundArithmeticRule;
import org.linqs.psl.model.rule.logical.AbstractGroundLogicalRule;
import org.linqs.psl.model.rule.logical.UnweightedGroundLogicalRule;
import org.linqs.psl.model.rule.logical.WeightedGroundLogicalRule;
import org.linqs.psl.reasoner.function.FunctionComparator;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

/**
 * A TermGenerator for simplified terms.
 * These terms are usually later serialized and sent off to some external optimizer.
 */
public class SimpleTermGenerator implements TermGenerator<SimpleTerm> {
	@Override
	public int generateTerms(GroundRuleStore ruleStore, TermStore<SimpleTerm> termStore) {
		int count = 0;

		for (GroundRule groundRule : ruleStore.getGroundRules()) {
			SimpleTerm term = createTerm(groundRule);
			if (term.size() > 0) {
				termStore.add(groundRule, term);
				count++;
			}
		}

		return count;
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
		boolean squared = !hard && (((WeightedGroundLogicalRule)rule).isSquared());
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
		boolean hard = (rule instanceof UnweightedGroundArithmeticRule);
		boolean squared = !hard && (((WeightedGroundArithmeticRule)rule).isSquared());
		double weight = hard ? -1 : ((WeightedGroundArithmeticRule)rule).getWeight();

		SimpleTerm term = new SimpleTerm(hard, squared, weight, 0.0);

		// We will have to switch around some signs depending on the comparison operator.
		// Remember that we don't have a equality comparator.
		boolean largerThan = FunctionComparator.LargerThan.equals(rule.getComparator());

		double[] coefficients = rule.getCoefficients();
		GroundAtom[] atoms = rule.getOrderedAtoms();

		// Add up all the atoms.
		for (int i = 0; i < coefficients.length; i++) {
			// Skip any special predicates.
			if (atoms[i].getPredicate() instanceof SpecialPredicate) {
				continue;
			}

			double modifier = largerThan ? -1.0 : 1.0;
			term.add(atoms[i], modifier * coefficients[i]);
		}

		// Add in the constant (sign swap since it starts on the RHS).
		double modifier = largerThan ? 1.0 : -1.0;
		term.addConstant(modifier * rule.getConstant());

		return term;
	}
}
