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

import java.util.HashMap;
import java.util.Map;

public class VariableFactory {
	private static int counter = 0;

	private Map<Integer, Variable> vars = new HashMap<>();

	// generate a hole of type t.
	public Variable getVar(Hole hole, DefVar v) {
		counter++;
		int id = counter;
		Variable var = new Variable(id, hole, v);
		vars.put(id, var);
		return var;
	}

	public Variable getVarById(int id) {
		return vars.get(id);
	}

	public void reset() {
		counter = 0;
	}
}
