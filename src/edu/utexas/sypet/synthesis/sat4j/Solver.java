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
package edu.utexas.sypet.synthesis.sat4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.TimeoutException;

import edu.utexas.sypet.synthesis.sat4j.Constraint.ConstraintType;

public class Solver {

	/**
	 * Members of Encoding class
	 */
	// protected IPBSolver solver;
	protected IPBSolver solver = null;
	protected ArrayList<Boolean> model;
	protected ArrayList<Integer> assumptions;
	protected Constraint objective_function;
	protected IConstr objective_blocking;
	protected int assumption_var;
	
	private static int MAX_ITERATIONS = 5;

	private static final Logger LOG = Logger.getLogger(Solver.class.getName());

	/**
	 * Constructors
	 */
	public Solver() {
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		model = new ArrayList<Boolean>();
		assumptions = new ArrayList<Integer>();
		objective_function = null;
		assumption_var = 0;
		objective_blocking = null;
	}

	/**
	 * Public methods
	 */
	public ArrayList<Boolean> getModel() {
		return model;
	}

	public void update(Constraint c) throws ContradictionException {
		addConstraint(c);
	}
	
	public int getMaxIterations(){
		return MAX_ITERATIONS;
	}

        public void setTimeout(int time){
	        solver.setTimeout(time);
        }
	
	public void setMaxIterations(int itn){
		MAX_ITERATIONS = itn;
	}

	public void build(PetrinetEncoding encoding) throws ContradictionException {

		// rebuild the solver
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		//solver.setTimeout(3600);

		// create variables in the solver
		solver.newVar(encoding.nVars());
		
		// add constraints
		for (int i = 0; i < encoding.nConstraints(); i++) {
			Constraint c = encoding.getConstraints().get(i);
			addConstraint(c);
		}

		// add objective function
		objective_function = encoding.getObjectiveFunctions();

//		LOG.info("Objective Function #" + encoding.getObjectiveId() + " Variables #" + solver.nVars() + " Constraints #"
//				+ solver.nConstraints());

	}
		
	public boolean solve() {
		try {
			if(solver.isSatisfiable()){
				solver.expireTimeout();
				int len = solver.model().length;
				Boolean[] modelArray = new Boolean[len];
				for (int i = 0; i < len; ++i) {
					modelArray[i] = solver.model(i + 1);
				}
				model = new ArrayList<Boolean>(Arrays.asList(modelArray));
				return true;
			}
			else return false;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean solve(PetrinetEncoding encoding) {

		int itn = 0;
		model.clear();
		
		if(assumption_var != 0){
			try {
				solver.addClause(new VecInt(new int[] {assumption_var}));
			} catch (ContradictionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			assumption_var = 0;
		}
		
		try {

			while (itn < MAX_ITERATIONS) {

				boolean res = false;

				//solver.setTimeout(3600); // Timeout for the SAT solver
				
				if (assumption_var == 0) {
					if (encoding.getVoidBlocker() != 0){
						res = solver.isSatisfiable(new VecInt(new int [] {-(encoding.getVoidBlocker())}));
						solver.expireTimeout();
					}
					else {
					 res = solver.isSatisfiable();
					 solver.expireTimeout();
					}
				}
				else {
					if (encoding.getVoidBlocker() != 0){
						res = solver.isSatisfiable(new VecInt(new int[] {-assumption_var, -(encoding.getVoidBlocker())}));
						solver.expireTimeout();
					}
					else {
						res = solver.isSatisfiable(new VecInt(new int[] {-assumption_var}));
						solver.expireTimeout();
					}
				}

				if (res) {
					
					//System.out.println("model at itn: " + itn);
					// saves the model
					int len = solver.model().length;
					Boolean[] modelArray = new Boolean[len];
					for (int i = 0; i < len; ++i) {
						modelArray[i] = solver.model(i + 1);
					}
					model = new ArrayList<Boolean>(Arrays.asList(modelArray));

					// updates the objective function
					if (objective_function.getSize() > 0) {

						int k = getOptimum(objective_function);
						//System.out.println("Objective value: " + k);
						
						if (assumption_var != 0){
							try {
								solver.addClause(new VecInt(new int[] {assumption_var}));
							} catch (ContradictionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
							

						int sum = 0;
						for (Integer l : objective_function.getCoefficients()) {
							if (l > 0)
								sum += l;
						}

						ArrayList<Variable> obj = new ArrayList<Variable>();
						obj.addAll(objective_function.getLiterals());

						ArrayList<Integer> coeff = new ArrayList<Integer>();
						coeff.addAll(objective_function.getCoefficients());

						Constraint c = new Constraint(obj, coeff, ConstraintType.LEQ, k - 1);
						
						assumption_var = solver.nVars()+1;
						solver.newVar(solver.nVars()+1);
						c.addLiteral(new FunctionVar(assumption_var-1, -1, null), -sum + k - 1);

						 try {
								 objective_blocking = addConstraint(c);
						 } catch (ContradictionException e) {

							 return true;
						 }
						
					} else {
						break;
					}

				} else {
					
					boolean blockerFault = false;
					
					try {
						
						// check if it is due to the voidBlocker
						for (int i = 0; i < solver.unsatExplanation().size(); i++){
							if (solver.unsatExplanation().get(i) == -encoding.getVoidBlocker()){
								blockerFault = true;
								break;
							}
						}
					    
					} catch(NullPointerException e) {

						if (!model.isEmpty()) {
							return true;
						} else
							return false;

					}
					
					if (blockerFault){
						// relax the constraint
						
						try {
							solver.addClause(new VecInt(new int[] { encoding.getVoidBlocker() }));
						} catch (ContradictionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						// upper limit on the number of relaxations
						if (encoding.getVoidLimit() < 2){
							Constraint c = encoding.createVoidConstraint(encoding.getVoidLimit()+1);
							solver.newVar(solver.nVars()+1);
							try {
								addConstraint(c);
							} catch (ContradictionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else 
							encoding.setVoidBlocker(0);
						
					} else {
						
						if (!model.isEmpty()) {
							return true;
						} else
							return false;
					}
					//System.out.println("UNSAT at itn: " + itn);
					
				}
				itn++;
			}

		} catch (TimeoutException e) {
			LOG.warning("Timeout in the SAT solver.");
			return false;
		}

		return true;
	}

	public IConstr addConstraint(Constraint c) throws ContradictionException {

		int size = c.getSize();
		int[] constraint = new int[size];
		int[] coefficients = new int[size];
		for (int j = 0; j < size; j++) {
			assert c.getLiterals() != null;
			assert c.getLiterals().get(j) != null : c.getLiterals().size() + " v.s." + j;
			constraint[j] = c.getLiterals().get(j).getSolverId() + 1;
			coefficients[j] = c.getCoefficients().get(j);
			assert(constraint[j] > 0 && constraint[j] <= solver.nVars());
		}
		switch (c.getType()) {
		case EQ:
			return solver.addExactly(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		case GEQ:
			return solver.addAtLeast(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		case LEQ:
			return solver.addAtMost(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		default:
			assert(false);
		}
		return null;
	}

	/**
	 * Protected methods
	 */
	protected void addObjectiveConstraints(PetrinetEncoding encoding) throws ContradictionException {

		boolean encode = false;

		Constraint c = encoding.getObjectiveFunctions();
		int size = 0;
		for (Integer coeff : c.getCoefficients()) {
			if (coeff != 0) {
				encode = true;
				size++;
			}
		}

		if (encode) {
			solver.setObjectiveFunction(convertObjectiveFunction(c, size));
		}

	}

	protected ObjectiveFunction convertObjectiveFunction(Constraint c, int size) {

		int[] objective = new int[size];
		BigInteger[] coefficients = new BigInteger[size];

		assert(c.getCoefficients().size() == c.getLiterals().size());

		int pos = 0;

		for (int i = 0; i < c.getSize(); i++) {
			if (c.getCoefficients().get(i) == 0)
				continue;
			objective[pos] = c.getLiterals().get(i).getSolverId() + 1;
			assert(objective[pos] > 0 && objective[pos] <= solver.nVars());
			coefficients[pos] = BigInteger.valueOf(c.getCoefficients().get(i));
			assert(c.getCoefficients().get(i) > 0);
			pos++;
		}

		return new ObjectiveFunction(new VecInt(objective), new Vec<BigInteger>(coefficients));
	}

	protected int getOptimum(Constraint c) {

		assert(solver.model().length > 0);

		int value = 0;
		for (int i = 0; i < c.getLiterals().size(); i++) {
			// Variables in SAT4J start with index 1
			int sat4j_variable = c.getLiterals().get(i).getSolverId();
			if (model.get(sat4j_variable)) {
				value += c.getCoefficients().get(i);
			}
		}
		return value;
	}

}
