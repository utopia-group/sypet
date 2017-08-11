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

import java.util.ArrayList;
import java.util.List;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;

public abstract class Encoding {

	protected ArrayList<Constraint> problem_constraints;
	protected ArrayList<Constraint> conflict_constraints;
	protected ArrayList<Variable> list_variables;
	protected Constraint objective_function;
	
	protected PetriNet petrinet;
	protected List<Place> inits;
	protected Place goal;
	protected int index_var = 0;

	
	public Encoding(PetriNet p, List<Place> initPlace, Place tgt){
		problem_constraints = new ArrayList<Constraint>();
		conflict_constraints = new ArrayList<Constraint>();
		list_variables = new ArrayList<Variable>();
		
		objective_function = new Constraint();
		
		petrinet = p;
		inits = initPlace;
		goal = tgt;
		
		index_var = 0;
	}
	
	public Integer nVars() {
		return list_variables.size();
	}
	
	public Integer nConstraints() {
		return problem_constraints.size();
	}

	public List<Constraint> getConstraints() {
		return problem_constraints;
	}

	public Constraint getObjectiveFunctions() {
		return objective_function;
	}
	
	public PetriNet getPetrinet() {
		return petrinet;
	}
	
	public List<Place> getInits() {
		return inits;
	}
	
	public abstract void build();
	public abstract void createVariables();
	public abstract void createConstraints();
	public abstract void createObjectiveFunctions();
	
	

}
