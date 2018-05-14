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
import java.util.Vector;

import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import edu.utexas.sypet.synthesis.model.Variable;

public class SketchSolver {

	protected List<Variable> variable_list;
	protected Variable[][] variable_matrix;
	protected int n_vars;
	protected int rows;
	protected int columns;
	protected IPBSolver solver;

	public SketchSolver(Variable[][] matrix, List<Variable> vList, int varNum, int rowNum, int colNum) {
		variable_matrix = matrix;
		n_vars = varNum;
		rows = rowNum;
		columns = colNum;
		variable_list = vList;
		solver = null;
	}

	// conversion to sat4j vector type
	protected IVecInt fromVectortoIVec(Vector<Integer> vec) {
		int[] constraint = new int[vec.size()];
		for (int i = 0; i < vec.size(); i++) {
			constraint[i] = vec.get(i);
		}
		return new VecInt(constraint);
	}

	public boolean createSketch() {

		// create new solver
		this.solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));

		// create variables in the solver
		solver.newVar(this.n_vars);
		
		// QUICK HACK to avoid unused variables
		for (int i = 1; i <= this.n_vars; i++){
			Vector<Integer> constraint = new Vector<Integer>();
			constraint.add(i);
			try {
				solver.addAtMost(fromVectortoIVec(constraint), 1);
			} catch (ContradictionException e) {
				return false;
			}
		}

		// add the row constraints -- each hole only contains exactly one
		// mapping
		for (int i = 0; i < this.rows; i++) {
			Vector<Integer> constraint = new Vector<Integer>();
			for (int j = 0; j < this.columns; j++) {
				Variable v = variable_matrix[i][j];
				// variable exists
				if (v != null) {
					constraint.add(v.getSolverId());
				}
			}

			try {
				this.solver.addExactly(fromVectortoIVec(constraint), 1);
			} catch (ContradictionException e) {
				// if the constraint system is unsatisfiable while adding
				// constraints
				//System.err.println("ContraditionException....");
//				e.printStackTrace();
				return false;
			}
		}

		// add the column constraints -- each primitive type maps to exactly one
		// mapping, and non-primitive maps to at least one mapping
		for (int j = 0; j < this.columns; j++) {
//			System.out.println("handling column: " + j);
			Vector<Integer> constraint = new Vector<Integer>();
			boolean primitive = false;
			for (int i = 0; i < this.rows; i++) {
				Variable v = variable_matrix[i][j];
				// variable exists
				if (v != null) {
					if (v.isPrimitive())
						primitive = true;
					constraint.add(v.getSolverId());
//					System.out.println("add v:" + v + " column: " + j);
				}
			}
			try {
				// assert(!constraint.isEmpty());
				if (primitive)
					this.solver.addExactly(fromVectortoIVec(constraint), 1);
				else
					this.solver.addAtLeast(fromVectortoIVec(constraint), 1);
			} catch (ContradictionException e) {
				// if the constraint system is unsatisfiable while adding
				// constraints
				//System.err.println("****Constraint contradiction!*****");
//				e.printStackTrace();
				return false;
			}
		}

		// API of the solver::
		// solver.addExactly(vector, k)
		// solver.addAtLeast(vector, k)
		// solver.addAtMost(vector, k);

		// at this point the solver will contain all constraints and it is in a
		// consistent state
		return true;
	}

	public boolean fillSketch() {

		try {
			boolean ok = solver.isSatisfiable();
			solver.expireTimeout();
			return ok;
		} catch (TimeoutException e) {
			// if the solver did not terminate in a given timeout
			return false;
		}
	}

	public boolean blockLastSketch() {

		assert(solver.model().length > 0);
		Vector<Integer> clause = new Vector<Integer>();
		for (int i = 0; i < solver.model().length; i++) {
			clause.add(-solver.model()[i]);
		}
		try {
			solver.addClause(fromVectortoIVec(clause));
		} catch (ContradictionException e) {
			return false;
		}

		return true;
	}

	public List<Variable> getModel() {
		assert(solver.model().length > 0);
		assert(solver.model().length == n_vars);

		ArrayList<Variable> variable_model = new ArrayList<Variable>();

		for (int i = 0; i < solver.model().length; i++) {
			if (solver.model()[i] > 0) {
				variable_model.add(variable_list.get(i));
			}
		}

		return variable_model;
	}

	public void system() {

		// can return false if unsat
		createSketch();

		while (true) {

			if (fillSketch()) {
				// SAT
			} else {
				// UNSAT
				break;
			}

			// SYNTHESIS
			// RUN TESTS

			// IF TESTS FAIL
			blockLastSketch();
			// OTHERWISE -- break

		}

	}

}