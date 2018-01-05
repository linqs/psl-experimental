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

import org.linqs.psl.experimental.optimizer.conic.program.LinearConstraint;
import org.linqs.psl.experimental.optimizer.conic.program.Variable;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.reasoner.function.ConstraintTerm;
import org.linqs.psl.reasoner.function.FunctionComparator;
import org.linqs.psl.reasoner.function.FunctionSummand;

import java.util.Map;

public class VariableConicProgramProxy extends ConicProgramProxy {
	protected Variable variable;
	protected ConstraintConicProgramProxy upperBound;

	public VariableConicProgramProxy(ConicTermStore termStore, GroundRule rule) {
		super(termStore, rule);

		variable = termStore.getProgram().createNonNegativeOrthantCone().getVariable();
		FunctionSummand summand = new FunctionSummand(1.0, new ConicReasonerSingleton(variable));
		ConstraintTerm con = new ConstraintTerm(summand, FunctionComparator.SmallerThan, 1.0);
		upperBound = new ConstraintConicProgramProxy(termStore, con, rule);
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public void remove() {
		Map<? extends Variable, Double> vars;
		double coeff;

		upperBound.remove();

		for (LinearConstraint lc : variable.getLinearConstraints()) {
			vars = lc.getVariables();
			if (vars.size() == 1) {
				lc.delete();
			} else {
				coeff = vars.get(variable);
				lc.setVariable(variable, 0.0);
				lc.setConstrainedValue(lc.getConstrainedValue() - coeff * variable.getValue());
			}
		}
		variable.getCone().delete();
	}
}
