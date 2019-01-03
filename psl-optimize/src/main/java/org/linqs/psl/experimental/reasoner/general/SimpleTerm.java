/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2019 The Regents of the University of California
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

import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.reasoner.term.ReasonerTerm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple terms for oprimization.
 * These terms are just simple summations and we want to minimize them.
 */
public class SimpleTerm implements ReasonerTerm {
    private final GroundRule groundRule;
    private double constant;
    private boolean hard;
    private boolean squared;
    private double weight;

    private List<RandomVariableAtom> atoms;
    private List<Double> coefficients;

    public SimpleTerm(boolean hard, boolean squared, double weight, double constant, GroundRule groundRule) {
        this.groundRule = groundRule;
        this.hard = hard;
        this.squared = squared;
        this.weight = weight;
        this.constant = constant;

        atoms = new ArrayList<RandomVariableAtom>();
        coefficients = new ArrayList<Double>();
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

    public void addConstant(ObservedAtom atom, double coefficient) {
        addConstant(atom.getValue() * coefficient);
    }

    public void addAtom(RandomVariableAtom atom, boolean isPositive) {
        addAtom(atom, isPositive ? 1.0 : -1.0);
    }

    public void addAtom(RandomVariableAtom atom, double coefficient) {
        atoms.add(atom);
        coefficients.add(coefficient);
    }

    public void add(GroundAtom atom, double coefficient) {
        if (atom instanceof ObservedAtom) {
            addConstant((ObservedAtom)atom, coefficient);
        } else {
            addAtom((RandomVariableAtom)atom, coefficient);
        }
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

    public RandomVariableAtom getAtom(int index) {
        return atoms.get(index);
    }

    public List<RandomVariableAtom> getAtoms() {
        return Collections.unmodifiableList(atoms);
    }

    public double getCoefficient(int index) {
        return coefficients.get(index).doubleValue();
    }

    public List<Double> getCoefficients() {
        return Collections.unmodifiableList(coefficients);
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
            builder.append(coefficients.get(i));
            builder.append(" * ");
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

    public GroundRule getGroundRule() {
        return groundRule;
    }
}
