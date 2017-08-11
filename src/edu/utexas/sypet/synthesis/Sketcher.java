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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import edu.utexas.hunter.model.CustomMethod;
import edu.utexas.sypet.synthesis.model.BinTree;
import edu.utexas.sypet.synthesis.model.DefVar;
import edu.utexas.sypet.synthesis.model.DefVarFactory;
import edu.utexas.sypet.synthesis.model.Hole;
import edu.utexas.sypet.synthesis.model.HoleFactory;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.model.Pent;
import edu.utexas.sypet.synthesis.model.Statement;
import edu.utexas.sypet.synthesis.model.StmtFactory;
import edu.utexas.sypet.synthesis.model.Variable;
import edu.utexas.sypet.synthesis.model.VariableFactory;
import edu.utexas.sypet.util.SootUtil;
import edu.utexas.sypet.util.SynthUtil;
import soot.Scene;
import soot.SootMethod;
import soot.Type;

public class Sketcher {

	protected List<Statement> stmts = new ArrayList<>();
	
	protected List<String> templates = new ArrayList<>();

	protected List<Pair<String, String>> args = new ArrayList<>();
	
	protected String retType;

	protected SketchSolver solver;
	
	protected boolean blocked = false;
	
	public Sketcher(List<String> tps, List<Pair<String, String>> vals, String ret) {
		templates = tps;
		args = vals;
		retType = ret;
	}

	public String fillHoles() {
		if (blocked)
			return "UNSAT";
		assert solver != null;
		boolean flag = solver.fillSketch();
		if (flag) {
			List<Variable> models = solver.getModel();
			// update holes per model.
			for (Variable v : models) {
				Hole h = v.getHole();
				h.setVar(v.getVar());
			}
			StringBuilder sb = new StringBuilder();
			for (Statement st : stmts) {
				if(st.isArg())
					continue;

				sb.append(st.toString());
			}
			// append return statement.
//			String retStmt = getRetStmt();
//			sb.append(retStmt);

			if (!solver.blockLastSketch())
				blocked = true;

			return sb.toString().replaceAll("\\$", ".");
		} else {
			return "UNSAT";
		}
	}
	
	public String getRetStmt() {
		ListIterator<Statement> li = stmts.listIterator(stmts.size());

		// Iterate in reverse.
		while (li.hasPrevious()) {
			Statement st = li.previous();
			if (st.getLhs() != null && st.getRetType().equals(retType)) {
				return "return " + st.getLhs() + ";";
			}
		}
		return "";
	}
	
	//return total number of holes.
	public int getHolesNum() {
		int total = 0;
		for (Statement stmt : stmts) {
			if (stmt.getArgHoles() != null)
				total += stmt.getArgHoles().size();
		}
		return total;
	}

	public boolean initSketch() {
		blocked = false;
		// code generation.
		HoleFactory hf = new HoleFactory();
		DefVarFactory vf = new DefVarFactory();
		VariableFactory ff = new VariableFactory();
		ff.reset();
		StmtFactory sf = new StmtFactory();
		sf.setHf(hf);
		stmts = new ArrayList<>();
		List<String> empty = new ArrayList<>();

		// Existing arguments
		for (Pair<String, String> p : args) {
			String srcType = p.val0;
			String srcVar = p.val1;
			Statement s1 = sf.getStmt("", srcType, empty);
			s1.setArg(true);
			DefVar dv1 = vf.getDefVar(s1.getRetType());
			dv1.setName(srcVar);
			s1.setLhs(dv1);
			stmts.add(s1);
			// System.out.println(s1 + " ret:" + s1.getRetType() + " holes:" +
			// s1.getArgHoles());
		}


		for (String methStr : templates) {
			boolean poly = false;
			String polyType = "";
			
			if(SootUtil.llTransitions.containsKey(methStr)) {
				//linkedlist transition.
				Pent<String, String, String, String,String> trio = SootUtil.llTransitions.get(methStr);
				String src = trio.val0;
				String tgt = trio.val1;
				List<String> paramList = new ArrayList<>();
				paramList.add(src);
				
				Statement s2 = sf.getStmt(methStr, tgt, paramList);
				s2.setStatic(false);
				s2.setClazz(src);
				s2.setSootMethod(methStr);
				DefVar dv2 = vf.getDefVar(s2.getRetType());
				s2.setLhs(dv2);
				stmts.add(s2);
				System.out.println("manually handle llTransition: ");
				continue;
			}
			
			if(SootUtil.BinTransitions.containsKey(methStr)) {
				//BinaryTree transition.
				Pair<BinTree, BinTree> trio = SootUtil.BinTransitions.get(methStr);
				BinTree src = trio.val0;
				BinTree tgt = trio.val1;
				List<String> paramList = new ArrayList<>();
				paramList.add(src.getType());
				
				Statement s2 = sf.getStmt(methStr, tgt.getType(), paramList);
				s2.setStatic(false);
				s2.setClazz(src.getType());
				s2.setSootMethod(methStr);
				DefVar dv2 = vf.getDefVar(s2.getRetType());
				s2.setLhs(dv2);
				stmts.add(s2);
				System.out.println("manually handle binTransition: ");
				continue;
			}
			
			//handle JDK.json
			if(methStr.contains("_sdk") || methStr.contains("_upper")) {
				Pair<String,String> stPair = SyPetService.sdkTypes.get(methStr);
				assert stPair != null;
				List<String> paramList = new ArrayList<>();
				paramList.add(stPair.val0);
				
				Statement s2 = sf.getStmt(methStr, stPair.val1, paramList);
				s2.setStatic(false);
				s2.setClazz(stPair.val0);
				s2.setSootMethod(methStr);
				DefVar dv2 = vf.getDefVar(s2.getRetType());
				s2.setLhs(dv2);
				stmts.add(s2);
				continue;
			}
			
			//hunter method.
			if (SootUtil.isHunterMethod(methStr)) {
				CustomMethod hunterMeth = SootUtil.getHunterMethod(methStr);
				this.handleHunterMethod(hunterMeth, sf, vf);
				continue;
			}
			
			assert Scene.v().containsMethod(methStr) : methStr;
			SootMethod meth = Scene.v().getMethod(methStr);
			String name = meth.getName();
			String declClazz = meth.getDeclaringClass().getName();

			String ret = meth.getReturnType().toString();
			// constructor
			if (meth.isConstructor()) {
				name = "new " + declClazz;
				ret = declClazz;
			}
			List<String> paramList = new ArrayList<>();
			if (!meth.isStatic() && !meth.isConstructor()) {
				String recv = meth.getDeclaringClass().getName();
				
				paramList.add(recv);
			}
			for (Type t : meth.getParameterTypes()) {
				String str = t.toString();
				
				if(poly) str = polyType;

				paramList.add(str);
			}

			Statement s2 = sf.getStmt(name, ret, paramList);
			s2.setStatic(meth.isStatic());
			s2.setClazz(declClazz);
			s2.setSootMethod(meth.getSignature());
			DefVar dv2 = vf.getDefVar(s2.getRetType());
			s2.setLhs(dv2);
			stmts.add(s2);
//			System.out.println(s2 + " args: " + s2.getArgTypes() + " ret:" + s2.getRetType() + " Holes:" + s2.getArgHoles());
		}
		
		//create return stmt.
		List<String> retList = new ArrayList<>();
		retList.add(retType);
		Statement retStmt = sf.getStmt("return", retType, retList);
		stmts.add(retStmt);

		// generate holes for rows
		List<Hole> rows = new ArrayList<>();
		List<DefVar> cols = new ArrayList<>();
		List<Variable> vars = new ArrayList<>();

		Map<Object, Integer> indexMap = new HashMap<>();
		for (Statement st : stmts) {
			int index = stmts.indexOf(st);
			rows.addAll(st.getArgHoles());
			for (Hole h : st.getArgHoles()) {
				indexMap.put(h, index);
			}
			if (st.getLhs() != null) {
				if (st.getLhs().getType().equals("void") || st.getName().equals("return"))
					continue;
				cols.add(st.getLhs());
				indexMap.put(st.getLhs(), index);
//				System.out.println("index: " +index + " stmt:" + st + " meth:" + st.getMethod() + " ret:" + st.getLhs().getType());
			}
		}

		int cnt = 0;
		int[][] table = new int[rows.size()][cols.size()];
		Variable[][] variable_matrix = new Variable[rows.size()][cols.size()];

		for (int row = 0; row < rows.size(); row++) {
			Hole h = rows.get(row);
			for (int col = 0; col < cols.size(); col++) {
				DefVar dv = cols.get(col);
				if (SynthUtil.compatible(dv.getType(), h.getType()) && (indexMap.get(dv) < indexMap.get(h))) {
					table[row][col] = 1;
					Variable v = ff.getVar(h, dv);
					v.setPrimitive(dv.isPrimitive());
					variable_matrix[row][col] = v;
					vars.add(v);
					cnt++;
				}
			}
		}

		// checking number of non-zero elements
		// System.out.println("#elements:" + cnt);
		// System.out.println("#vars:" + vars);

		solver = new SketchSolver(variable_matrix, vars, cnt, rows.size(), cols.size());
		boolean hasSketh = solver.createSketch();
		return hasSketh;
	}
	
	private void handleHunterMethod(CustomMethod meth, StmtFactory sf, DefVarFactory vf) {
		String name = meth.getName();
		String declClazz = meth.getDeclaredClass();

		String ret = meth.getRetType();
		// constructor
		if (meth.isConstructor()) {
			name = "new " + declClazz;
			ret = declClazz;
		}
		List<String> paramList = new ArrayList<>();
		if (!meth.isStaticMethod() && !meth.isConstructor()) {
			String recv = meth.getDeclaredClass();
			paramList.add(recv);
		}
		paramList.addAll(meth.getParams());

		Statement s2 = sf.getStmt(name, ret, paramList);
		s2.setStatic(meth.isStaticMethod());
		s2.setClazz(declClazz);
		s2.setSootMethod(meth.getSignature());
		DefVar dv2 = vf.getDefVar(s2.getRetType());
		s2.setLhs(dv2);
		stmts.add(s2);
	}
}
