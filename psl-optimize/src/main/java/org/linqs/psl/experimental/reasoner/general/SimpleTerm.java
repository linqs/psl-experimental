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
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.UnweightedGroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.reasoner.function.AtomFunctionVariable;
import org.linqs.psl.reasoner.function.ConstantNumber;
import org.linqs.psl.reasoner.function.ConstraintTerm;
import org.linqs.psl.reasoner.function.FunctionSingleton;
import org.linqs.psl.reasoner.function.FunctionSum;
import org.linqs.psl.reasoner.function.FunctionSummand;
import org.linqs.psl.reasoner.function.FunctionTerm;
import org.linqs.psl.reasoner.function.MaxFunction;
import org.linqs.psl.reasoner.function.PowerOfTwo;
import org.linqs.psl.reasoner.term.Term;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A TermGenerator for terms meant to be serialized via JSON.
 */
public class SimpleTerm implements Term {
	private double constant;
	private boolean hard;
	private boolean squared;
	private double weight;

	private List<RandomVariableAtom> atoms;
	private List<Boolean> signs;

	public SimpleTerm(boolean hard, boolean squared, double weight, double constant) {
		this.hard = hard;
		this.squared = squared;
		this.weight = weight;
		this.constant = constant;

		atoms = new ArrayList<RandomVariableAtom>();
		signs = new ArrayList<Boolean>();
	}

	public void addConstant(double value) {
		constant += value;
	}

	public void addConstant(ObservedAtom atom, boolean isPositive) {
		if (isPositive) {
			constant += atom.getValue();
		} else {
			constant -= atom.getValue();
		}
	}

	public void addAtom(RandomVariableAtom atom, boolean isPositive) {
		atoms.add(atom);
		signs.add(isPositive);
	}

	public double getConstant() {
		return constant;
	}

	public boolean isHard() {
		return hard;
	}

	public boolean isSquared() {
		return squared;
	}

	public double getWeight() {
		return weight;
	}

	public List<RandomVariableAtom> getAtoms() {
		return Collections.unmodifiableList(atoms);
	}

	public List<Boolean> getSigns() {
		return Collections.unmodifiableList(signs);
	}

	public int size() {
		return atoms.size();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		if (!hard) {
			builder.append("" + weight + " * ( ");
		}

		if (squared) {
			builder.append("( ");
		}

		builder.append(constant);

		for (int i = 0; i < atoms.size(); i++) {
			if (signs.get(i).booleanValue()) {
				builder.append(" + ");
			} else {
				builder.append(" - ");
			}

			builder.append(atoms.get(i));
		}

		if (squared) {
			builder.append(" ) ^2");
		}

		if (!hard) {
			builder.append(" )");
		}
			
		return builder.toString();
	}
}
