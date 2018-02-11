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

import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.experimental.optimizer.conic.ConicProgramSolver;
import org.linqs.psl.experimental.optimizer.conic.program.ConicProgram;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.reasoner.function.AtomFunctionVariable;
import org.linqs.psl.reasoner.term.TermStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A TermStore for use by Conic reasoners.
 * This term store will handle loading ConicProgramProxy's into the solver/program.
 */
public class ConicTermStore implements TermStore<ConicProgramProxy> {
	/**
	 * Prefix of property keys used by this class.
	 */
	public static final String CONFIG_PREFIX = "conictermstore";

	/**
	 * Key for {@link org.linqs.psl.config.Factory} or String property.
	 *
	 * Should be set to a {@link org.linqs.psl.experimental.optimizer.conic.ConicProgramSolver}.
	 * The ConicReasoner will use this to solver which will then be used for inference.
	 */
	public static final String CPS_KEY = CONFIG_PREFIX + ".conicprogramsolver";
	public static final String CPS_DEFAULT = "org.linqs.psl.experimental.optimizer.conic.ipm.HomogeneousIPM";

	private ConicProgram program;
	private ConicProgramSolver solver;

	private ArrayList<ConicProgramProxy> terms;
	private Map<GroundRule, ConicProgramProxy> groundTermMapping;
	private Map<AtomFunctionVariable, VariableConicProgramProxy> vars;

	public ConicTermStore(ConfigBundle config) {
		program = new ConicProgram();

		solver = (ConicProgramSolver)config.getNewObject(CPS_KEY, CPS_DEFAULT);
		solver.setConicProgram(program);

		terms = new ArrayList<ConicProgramProxy>();
		groundTermMapping = new HashMap<GroundRule, ConicProgramProxy>();
		vars = new HashMap<AtomFunctionVariable, VariableConicProgramProxy>();
	}

	public ConicProgram getProgram() {
		return program;
	}

	public ConicProgramSolver getSolver() {
		return solver;
	}

	public VariableConicProgramProxy getVarProxy(AtomFunctionVariable variable) {
		VariableConicProgramProxy proxy = vars.get(variable);
		if (proxy == null) {
			proxy = new VariableConicProgramProxy(this, null);
			vars.put(variable, proxy);
		}

		return proxy;
	}

	public void updateAtoms() {
		for (Map.Entry<AtomFunctionVariable, VariableConicProgramProxy> entry : vars.entrySet()) {
			entry.getKey().setValue(entry.getValue().getVariable().getValue());
		}
	}

	@Override
	public void add(GroundRule rule, ConicProgramProxy term) {
		terms.add(term);
		groundTermMapping.put(rule, term);
	}

	@Override
	public void updateWeight(WeightedGroundRule rule) {
		ConicProgramProxy proxy = groundTermMapping.get(rule);
		if (!(proxy instanceof FunctionConicProgramProxy)) {
			throw new IllegalStateException("Expected a FunctionConicProgramProxy.");
		}

		((FunctionConicProgramProxy)proxy).updateGroundKernelWeight(rule);
	}

	@Override
	public int size() {
		return terms.size();
	}

	@Override
	public void ensureCapacity(int capacity) {
		assert(capacity >= 0);

		if (capacity == 0) {
			return;
		}

		terms.ensureCapacity(capacity);

		// If the map is empty, then just reallocate it
		// (since we can't add capacity).
		if (groundTermMapping.size() == 0) {
			// The default load factor for Java HashMaps is 0.75.
			groundTermMapping = new HashMap<GroundRule, ConicProgramProxy>((int)(capacity / 0.75));
		}

		if (vars.size() == 0) {
			// Assume 2 atoms per term.
			vars = new HashMap<AtomFunctionVariable, VariableConicProgramProxy>((int)(capacity * 2 / 0.75));
		}
	}

	@Override
	public void close() {
		clear();

		terms = null;
		groundTermMapping = null;
		vars = null;
		program = null;
		solver = null;
	}

	@Override
	public void clear() {
		if (terms != null) {
			terms.clear();
		}

		if (groundTermMapping != null) {
			groundTermMapping.clear();
		}

		if (vars != null) {
			vars.clear();
		}
	}

	@Override
	public ConicProgramProxy get(int index) {
		return terms.get(index);
	}

	@Override
	public List<Integer> getTermIndices(WeightedGroundRule rule) {
		ConicProgramProxy proxy = groundTermMapping.get(rule);
		if (proxy == null) {
			throw new IllegalArgumentException("Rule not found.");
		}

		// HACK(eriq): This is costly, but no one uses Conic solvers.
		//  If people start using it again, make it not awefull.
		return Arrays.asList(new Integer(terms.indexOf(proxy)));
	}

	@Override
	public Iterator<ConicProgramProxy> iterator() {
		return terms.iterator();
	}
}
