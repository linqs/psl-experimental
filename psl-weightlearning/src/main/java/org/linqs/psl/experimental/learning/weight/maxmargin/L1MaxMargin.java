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
package org.linqs.psl.experimental.learning.weight.maxmargin;

import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Max-margin learning with l1 loss function.
 *
 * @author Stephen Bach <bach@cs.umd.edu>
 * @author Bert Huang <bert@cs.umd.edu>
 */
public class L1MaxMargin extends MaxMargin {

	private static final Logger log = LoggerFactory.getLogger(L1MaxMargin.class);

	/**
	 * Prefix of property keys used by this class.
	 *
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "l1maxmargin";

	/**
	 * Key for LossBalancingType enum property. Determines the type of loss
	 * balancing MaxMargin will use.
	 *
	 * @see LossBalancingType
	 */
	public static final String BALANCE_LOSS_KEY = CONFIG_PREFIX + ".balanceloss";
	/** Default value for BALANCE_LOSS_KEY */
	public static final LossBalancingType BALANCE_LOSS_DEFAULT = LossBalancingType.NONE;

	/** Types of loss balancing L1MaxMargin can use during learning */
	public enum LossBalancingType {
		/** No loss balancing. All LossAugmentingGroundKernels weighted as -1.0. */
		NONE,
		/**
		 * Weights of LossAugmentingGroundKernels for true (false) ObservedAtoms
		 * are -2 * number of true (false) ObservedAtoms / total ObservedAtoms.
		 */
		CLASS_WEIGHTS,
		/**
		 * Weights of LossAugmentingGroundKernels for true (false) ObservedAtoms
		 * are -2 * number of false (true) ObservedAtoms / total ObservedAtoms.
		 */
		REVERSE_CLASS_WEIGHTS;
	}

	private final LossBalancingType balanceLoss;
	private double obsvTrueWeight, obsvFalseWeight;
	private List<LossAugmentingGroundRule> lossKernels, nonExtremeLossKernels;

	public L1MaxMargin(Model model, Database rvDB, Database observedDB,
			ConfigBundle config) {
		super(model, rvDB, observedDB, config);
		balanceLoss = (LossBalancingType) config.getEnum(BALANCE_LOSS_KEY, BALANCE_LOSS_DEFAULT);
	}

	@Override
	protected void setupSeparationOracle() {
		/* Determines weights of LossAugmentingGroundKernels */
		if (LossBalancingType.NONE.equals(balanceLoss)) {
			obsvTrueWeight = -1.0;
			obsvFalseWeight = -1.0;
		}
		else {
			/*
			 * Counts positive vs negative ground truth atoms in order to weight
			 * LossAugmentingGroundKernels
			 */
			int posAtoms = 0;
			for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet())
				if (e.getValue().getValue() == 1.0)
					posAtoms++;
				else if (e.getValue().getValue() != 0.0)
					throw new IllegalStateException("Cannot perform loss balancing " +
							"when some ground truth atoms have value other than 1.0 or 0.0.");
			double posRatio = (double) posAtoms / (double) trainingMap.getTrainingMap().size();

			if (LossBalancingType.CLASS_WEIGHTS.equals(balanceLoss)) {
				obsvTrueWeight = -2 * posRatio;
				obsvFalseWeight = -2 - 2 * obsvTrueWeight;
			}
			else if (LossBalancingType.REVERSE_CLASS_WEIGHTS.equals(balanceLoss)) {
				obsvFalseWeight = -2 * posRatio;
				obsvTrueWeight = -2 - 2 * obsvFalseWeight;
			}
			else
				throw new IllegalStateException("Unrecognized LossBalancingType.");
		}

		/* Sets up loss augmenting ground kernels */
		log.info("Weighting loss of positive (value = 1.0) examples by {} " +
				"and negative examples by {}", obsvTrueWeight, obsvFalseWeight);
		lossKernels = new ArrayList<LossAugmentingGroundRule>(trainingMap.getTrainingMap().size());
		nonExtremeLossKernels = new ArrayList<LossAugmentingGroundRule>();
		for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet()) {
			double truth = e.getValue().getValue();
			LossAugmentingGroundRule groundRule;

			/* If ground truth is at 1.0 or 0.0, sets up ground kernel without planning to change it */
			if (truth == 1.0 || truth == 0.0) {
				double weight = (truth == 1.0) ? obsvTrueWeight : obsvFalseWeight;
				groundRule = new LossAugmentingGroundRule(e.getKey(), truth, weight);
			}
			/* Else, does a little more to check it and change it later */
			else {
				if (truth >= 0.5)
					groundRule = new LossAugmentingGroundRule(e.getKey(), 1.0, obsvTrueWeight);
				else
					groundRule = new LossAugmentingGroundRule(e.getKey(), 1.0, -1 * obsvTrueWeight);

				nonExtremeLossKernels.add(groundRule);
			}

			groundRuleStore.addGroundRule(groundRule);
			lossKernels.add(groundRule);
		}
	}

	@Override
	protected void runSeparationOracle() {
		int optimizationCount = 0;
		boolean rerunOptimization = true;
		while (rerunOptimization && optimizationCount < maxIter) {
			reasoner.optimize(termStore);

			rerunOptimization = false;
			for (LossAugmentingGroundRule groundRule : nonExtremeLossKernels) {
				double currentValue = groundRule.getAtom().getValue();
				double truth = trainingMap.getTrainingMap().get(groundRule.getAtom()).getValue();
				if (currentValue > truth && groundRule.getWeight() >= 0) {
					groundRule.setWeight(obsvTrueWeight);
					rerunOptimization = true;
				}
				else if (currentValue < truth && groundRule.getWeight() < 0) {
					groundRule.setWeight(-1 * obsvTrueWeight);
					termGenerator.updateWeights(groundRuleStore, termStore);
					rerunOptimization = true;
				}
			}

			optimizationCount++;
		}

		log.info("Separation oracle performed {} optimizations.", optimizationCount);
	}

	@Override
	protected double evaluateLoss() {
		double loss = 0.0;
		for (LossAugmentingGroundRule groundRule : lossKernels) {
			double currentValue = groundRule.getAtom().getValue();
			double truth = trainingMap.getTrainingMap().get(groundRule.getAtom()).getValue();
			double lossTerm = groundRule.getWeight() * Math.abs(truth - currentValue);
			if (lossTerm <= 0)
				lossTerm *= -1;
			loss += lossTerm;
		}
		return loss;
	}

	@Override
	protected void tearDownSeparationOracle() {
		for (LossAugmentingGroundRule groundRule : lossKernels)
			groundRuleStore.removeGroundRule(groundRule);

		lossKernels.clear();
		nonExtremeLossKernels.clear();
	}

}
