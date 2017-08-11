/*
 * Copyright (C) 2017 The SyPet Authors
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
package edu.utexas.sypet.synthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sat4j.specs.ContradictionException;

import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.sat4j.PetrinetEncoding;
import edu.utexas.sypet.synthesis.sat4j.Solver;
import edu.utexas.sypet.synthesis.sat4j.PetrinetEncoding.Option;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;

public class PathFinder {
	protected PetrinetEncoding encoding;
	protected Solver solver;
	protected boolean OK;

	protected int solver_limit = 5;

	protected List<String> clones;

	protected Option objectiveOption;
	protected List<Pair<String, String>> vars;
	protected String tgt;

	public static boolean isHyper = false;

	public PathFinder(PetriNet p, List<Place> inits, Place goal, int timeline, int tokens) {

		objectiveOption = Option.AT_LEAST_ONE;

		solver = new Solver();
		solver.setMaxIterations(solver_limit);
		encoding = new PetrinetEncoding(p, inits, goal);

		encoding.setBound(timeline, tokens);
		encoding.build();
	}

	public PathFinder(PetriNet p, List<Place> inits, Place goal, int timeline, int tokens, Set<String> white,
			List<String> clones) {
		solver = new Solver();
		encoding = new PetrinetEncoding(p, inits, goal);
		encoding.setBound(timeline, tokens);
		encoding.setClones(clones);
		encoding.build();

		try {
			solver.build(encoding);
			OK = true;
		} catch (ContradictionException e) {
			OK = false;
		}
	}

	public PathFinder(PetriNet p, List<Place> inits, Place goal, int timeline, int tokens, List<String> clones,
			Option objOption, int solverLimit) {

		solver = new Solver();
		solver.setMaxIterations(solverLimit);
		solver_limit = solverLimit;

		encoding = new PetrinetEncoding(p, inits, goal);

		objectiveOption = objOption;

		encoding.setBound(timeline, tokens);
		encoding.setClones(clones);
		this.clones = clones;

		encoding.setObjectiveOption(objOption);
		encoding.build();

		try {
			solver.build(encoding);
			OK = true;
		} catch (ContradictionException e) {
			OK = false;
		}
	}

	public List<Pair<String, String>> getVars() {
		return vars;
	}

	public String getTgt() {
		return tgt;
	}

	public void setVars(List<Pair<String, String>> vars) {
		this.vars = vars;
	}

	public void setTgt(String tgt) {
		this.tgt = tgt;
	}

	public void setClones(List<String> clones) {
		encoding.setClones(clones);
	}

	public PetrinetEncoding getEncoding() {
		return encoding;
	}

	public void setObjective(Option opt) {
		encoding.setObjectiveOption(opt);
		objectiveOption = opt;
	}

	public Option getObjective() {
		return objectiveOption;
	}

	public Solver getSolver() {
		return solver;
	}

	public List<String> get() {

		if (!OK)
			return new ArrayList<String>();

		boolean res = solver.solve(encoding);

		if (res) {
			List<String> list = encoding.saveModel(solver);

			// block previous model.
			try {
				solver.addConstraint(encoding.blockPath());
			} catch (ContradictionException e) {
				OK = false;
			}
			return list;
		} else {
			return new ArrayList<String>();
		}
	}

	public PetriNet getPetriNet() {
		return encoding.getPetriNet();
	}

}
