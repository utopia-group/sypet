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

import uniol.apt.adt.pn.Transition;

/**
 * Each FunctionVar denote a Pair(f,t): f is the function(transition) and t is
 * the timestamp
 * 
 * @author yufeng
 *
 */
public class FunctionVar extends Variable {

	private Transition transition;

	public FunctionVar(int id, int time, Transition t) {
		super(id, time);
		transition = t;
	}

	public Transition getTransition() {
		return transition;
	}

	public void setTransition(Transition t) {
		this.transition = t;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(transition.getId()).append(" time:").append(time).append(" solverId:" + (solverId+1));
		return sb.toString();
	}

}
