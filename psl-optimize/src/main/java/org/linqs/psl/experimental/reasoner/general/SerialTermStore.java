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

import org.linqs.psl.reasoner.term.Term;
import org.linqs.psl.reasoner.term.TermStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * A TermStore that can write out its terms to a file.
 */
public interface SerialTermStore<E extends Term> extends TermStore<E> {
	/**
	 * Write out the optimiztion problem
	 * (variables, objective, and constraints) to a file.
	 */
	public void serialize(BufferedWriter writer) throws IOException;

	/**
	 * Read a file describing the solution and
	 * update all the variables (ground atoms).
	 */
	public void deserialize(BufferedReader reader) throws IOException;
}
