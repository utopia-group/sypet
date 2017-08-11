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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StmtFactory {

	private static int counter = 0;
	
	private HoleFactory hf;

	private Map<Integer, Statement> stmts = new HashMap<>();

	public void setHf(HoleFactory hf) {
		this.hf = hf;
	}

	// generate a hole of type t.
	public Statement getStmt(String name, String retType, List<String> args) {
		counter++;
		int id = counter;
		List<Hole> holes = new ArrayList<>();
		for (String arg : args) {
			// generate fresh new holes.
			Hole h = hf.getHole(arg);
			holes.add(h);
		}
		
		Statement stmt = new Statement(id, name, retType, args, holes);
		stmts.put(id, stmt);
		return stmt;
	}

	public Statement getStmtById(int id) {
		return stmts.get(id);
	}
}
