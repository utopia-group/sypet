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

public class Constraint {
	
	public static enum ConstraintType {
		LEQ, GEQ, EQ
	}
	
	public static enum EncodingType {
		INIT, GOAL, ONE_TRANSACTION, FIRE_TRANSACTION, FRAME_AXIOMS
	}
	
	/**
	 * Members of Encoding class
	 */
	protected List<Variable> literals;
	protected List<Integer> coefficients;
	protected ConstraintType type;
	protected Integer rhs;
	protected EncodingType enctype;
	
	/**
	 * Constructors
	 */
	public Constraint(List<Variable> literals, List<Integer> coefficients, ConstraintType type, Integer rhs){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.coefficients = coefficients;
		this.type = type;
		this.rhs = rhs;
	}
	
	public Constraint(List<Variable> literals, List<Integer> coefficients, ConstraintType type, Integer rhs, EncodingType enctype){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.coefficients = coefficients;
		this.type = type;
		this.rhs = rhs;
		this.enctype = enctype;
	}

	
	public Constraint(List<Variable> literals, ConstraintType type, Integer rhs){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.type = type;
		
		coefficients = new ArrayList<Integer>();
		for (int i = 0; i < literals.size(); i++)
			coefficients.add(1);
		
		this.rhs = rhs;
	}
	
	public Constraint(List<Variable> literals, ConstraintType type, Integer rhs, EncodingType enctype){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.type = type;
		this.enctype = enctype;
		
		coefficients = new ArrayList<Integer>();
		for (int i = 0; i < literals.size(); i++)
			coefficients.add(1);
		
		this.rhs = rhs;
	}
	
	public Constraint(){
		this.literals = new ArrayList<Variable>();
		this.coefficients = new ArrayList<Integer>();
		this.type = ConstraintType.GEQ;
		this.rhs = 0;
	}
	
	/**
	 * Public methods
	 */
	public void addLiteral(Variable v, int coeff){
		literals.add(v);
		coefficients.add(coeff);
	}
	
	public List<Variable> getLiterals(){
		return literals;
	}
	
	public List<Integer> getCoefficients(){
		return coefficients;
	}
	
	public ConstraintType getType(){
		return type;
	}
	
	public int getRhs(){
		return rhs;
	}
	
	public void setRhs(int rhs){
		this.rhs = rhs;
	}
	
	public void setType(ConstraintType type){
		this.type = type;
	}
	
	public int getSize(){
		return literals.size();
	}
	
	public EncodingType getEncodingType(){
		return enctype;
	}
	
	public void print(){
		assert (literals.size() == coefficients.size());
		int pos = 0;
		for (Variable v : literals){
			System.out.print(coefficients.get(pos) + " x" + (v.getSolverId()+1) + " [" + v.toString() + "] ");
			pos++;
		}
		if(type == ConstraintType.EQ)
			System.out.print("=");
		else if (type == ConstraintType.GEQ)
			System.out.print(">=");
		else if (type == ConstraintType.LEQ)
			System.out.print("<=");
		
		System.out.println(" " + rhs);
	}
}