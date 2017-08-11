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
package edu.utexas.sypet.synthesis.model;

public class Variable {

	public Variable(int solId, Hole h, DefVar v) {
		hole = h;
		var = v;
		solverId = solId;
		id = solId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSolverId() {
		return solverId;
	}

	public void setSolverId(int solverId) {
		this.solverId = solverId;
	}

	public Hole getHole() {
		return hole;
	}

	public void setHole(Hole h) {
		this.hole = h;
	}

	public DefVar getVar() {
		return var;
	}

	public void setVar(DefVar v) {
		this.var = v;
	}

	public String toString() {
		return solverId + "|" + hole + "|" + var;
	}

	private int id;
	private int solverId;
	private Hole hole;
	private DefVar var;
	
	public boolean isPrimitive() {
		return isPrimitive;
	}

	public void setPrimitive(boolean isPrimitive) {
		this.isPrimitive = isPrimitive;
	}

	private boolean isPrimitive;
}
