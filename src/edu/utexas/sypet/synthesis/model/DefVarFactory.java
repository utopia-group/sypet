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

public class DefVarFactory {
	
	private static int counter = 0;
	
	private static String prefix = "sypet_var";

	private Map<Integer, DefVar> defVars = new HashMap<>();

	// generate a hole of type t.
	public DefVar getDefVar(String t) {
		String str = getHunterVar();
		DefVar var = new DefVar(t, str);
		defVars.put(counter, var);
		return var;
	}

	public DefVar getDefVarById(int id) {
		return defVars.get(id);
	}
	
	public static String getHunterVar() {
		counter++;
		int id = counter;
		String str = prefix + id;
		return str;
	}
	
	public static void reset() {
		counter = 0;
	}
}
