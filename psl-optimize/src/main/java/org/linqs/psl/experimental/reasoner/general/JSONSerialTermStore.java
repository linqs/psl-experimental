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
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.reasoner.term.MemoryTermStore;
import org.linqs.psl.reasoner.term.TermStore;

import com.google.gson.Gson;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A TermStore that is meant to serialize/deserialize the optimization
 * problem to/from JSON.
 */
public class JSONSerialTermStore
		extends MemoryTermStore<SimpleTerm> implements SerialTermStore<SimpleTerm> {
	private BidiMap<RandomVariableAtom, Integer> variableIds;

	public JSONSerialTermStore() {
		super();

		variableIds = new DualHashBidiMap<RandomVariableAtom, Integer>();
	}

	@Override
	public void serialize(BufferedWriter writer) {
		List<OutputTerm> objectiveSummands = new ArrayList<OutputTerm>();
		List<OutputTerm> constraints = new ArrayList<OutputTerm>();

		for (SimpleTerm term : this) {
			if (term.isHard()) {
				constraints.add(new OutputTerm(term));
			} else {
				objectiveSummands.add(new OutputTerm(term));
			}
		}

		Output output = new Output(variableIds.values(), objectiveSummands, constraints);

		Gson gson = new Gson();
		gson.toJson(output, writer);
	}

	@Override
	public void deserialize(BufferedReader reader) {
		Gson gson = new Gson();
		Input input = gson.fromJson(reader, Input.class);

		// Update the random variable atoms with their new values.
		for (Map.Entry<Integer, Double> entry : input.solution.entrySet()) {
			variableIds.getKey(entry.getKey()).setValue(entry.getValue().doubleValue());
		}
	}

	@Override
	public void add(GroundRule rule, SimpleTerm term) {
		for (RandomVariableAtom atom : term.getAtoms()) {
			if (!variableIds.containsKey(atom)) {
				variableIds.put(atom, variableIds.size());
			}
		}

		super.add(rule, term);
	}

	@Override
	public void clear() {
		variableIds.clear();
		super.clear();
	}

	@Override
	public void close() {
		clear();
		super.close();
	}

	private static class Output {
		public Set<Integer> variables;

		public List<OutputTerm> objectiveSummands;

		// All formulated such that the summation of each term is <= 0.
		public List<OutputTerm> constraints;

		public Output(Set<Integer> variables, List<OutputTerm> objectiveSummands, List<OutputTerm> constraints) {
			this.variables = variables;
			this.objectiveSummands = objectiveSummands;
			this.constraints = constraints;
		}
	}

	private class OutputTerm {
		public double constant;
		public int[] variables;
		public double[] coefficients;
		public boolean squared;
		public double weight;

		public OutputTerm(double constant, int[] variables, double[] coefficients,
				boolean squared, double weight) {
			this.constant = constant;
			this.variables = variables;
			this.coefficients = coefficients;
			this.squared = squared;
			this.weight = weight;
		}

		public OutputTerm(SimpleTerm term) {
			variables = new int[term.size()];
			coefficients = new double[term.size()];

			List<RandomVariableAtom> rawAtoms = term.getAtoms();
			List<Double> rawCoefficients = term.getCoefficients();
			for (int i = 0; i < term.size(); i++) {
				variables[i] = variableIds.get(rawAtoms.get(i)).intValue();
				coefficients[i] = rawCoefficients.get(i).doubleValue();
			}

			constant = term.getConstant();
			squared = term.isSquared();
			weight = term.getWeight();
		}
	}

	private static class Input {
		public Map<Integer, Double> solution;
	}
}
