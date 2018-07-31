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

import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.reasoner.term.MemoryTermStore;
import org.linqs.psl.reasoner.term.TermStore;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A TermStore that is meant to serialize to the LibSVM data format.
 * Note that deserialization is currently not supported.
 * The format written will differ from the LibSVM format in the following ways:
 *  - The first value will be the rule weight, not a class label.
 *  - There will be a zero-indexed variable, which will be the coefficient for each term.
 *  - If the ground rule is squared (and therefore weighted), the line will end with "# ^2".
 *  - If the ground rule is hard (unweighted), the weight will be 0 and the line will end with "# .".
 */
public class LibSVMSerialTermStore
		extends MemoryTermStore<SimpleTerm> implements SerialTermStore<SimpleTerm> {
	private Map<RandomVariableAtom, Integer> variableIds;

	public LibSVMSerialTermStore() {
		super();

		variableIds = new HashMap<RandomVariableAtom, Integer>();
	}

	@Override
	public void serialize(BufferedWriter writer) throws IOException {
		for (SimpleTerm term : this) {
			writer.write(serializeTerm(term));
			writer.newLine();
		}
	}

	@Override
	public void deserialize(BufferedReader reader) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(GroundRule rule, SimpleTerm term) {
		for (RandomVariableAtom atom : term.getAtoms()) {
			if (!variableIds.containsKey(atom)) {
				// Note that we are adding one to all the indexes to reserve 0 for the constant.
				variableIds.put(atom, variableIds.size() + 1);
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

	private String serializeTerm(SimpleTerm term) {
		StringBuilder builder = new StringBuilder(term.size() * 4 + 5);

		// Weight
		if (term.isHard()) {
			builder.append("0");
		} else {
			builder.append(term.getWeight());
		}
		builder.append(" ");

		// Constant
		builder.append("0:");
		builder.append(term.getConstant());

		// Variables
		for (int i = 0; i < term.size(); i++) {
			builder.append(" ");
			builder.append(variableIds.get(term.getAtom(i)));
			builder.append(":");
			builder.append(term.getCoefficient(i));
		}

		// Final comment (hard or squared).
		if (term.isHard()) {
			builder.append(" # .");
		} else if (term.isSquared()) {
			builder.append(" # ^2");
		}

		return builder.toString();
	}
}
