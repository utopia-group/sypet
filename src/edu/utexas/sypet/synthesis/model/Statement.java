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

import java.util.List;

import edu.utexas.sypet.util.SootUtil;

public class Statement {
	// id
	private int id;

	// name of the callee.
	private String name;

	// is static method
	private boolean isStatic = false;

	// return type.
	private String retType;

	// Holes list.
	private List<Hole> argHoles;

	// Type of argument list.
	private List<String> argTypes;

	// Variable in the lhs. Optional.
	private DefVar lhs;
	
	//declaring class
	private String clazz;
	
	private String meth;
	
	private static int cnt = 1;

	private boolean isArg = false;
	
	private String template = 
			"@tgt @lhs = null;"
			+"if(@arg != null) {"
			+ "@src cp@cnt = @arg;"
			+ "@tgt current@cnt = new @tgt(@arg.@val);"
			+ "@tgt previous@cnt = current@cnt;"
			+ "@lhs = current@cnt;"
			+ "while (cp@cnt != null) {"
			+ "cp@cnt = cp@cnt.@next1;"
			+ "if (cp@cnt != null) {current@cnt = new @tgt(cp@cnt.@val); previous@cnt.@next2 = current@cnt; previous@cnt = current@cnt;}"
			+ "else previous@cnt.@next2 = null;"
			+ "}}";
	
	private String binTemplate = ""
			+ "@tgtBin @lhs = null;"
			+ "if (@arg != null) {"
			+ "java.util.HashMap<@srcBin, @tgtBin> nodes = new java.util.HashMap<>();"
			+ "java.util.Queue<@srcBin> working = new java.util.LinkedList<>();"
			+ "working.add(@arg);"
			+ "while (!working.isEmpty()){"
			+ "@srcBin top = working.remove();"
			+ "@tgtBin bt = new @tgtBin(top.@srcId);"
			+ "nodes.put(top, bt);"
			+ "if (top.@srcLeft != null)"
			+ "working.add(top.@srcLeft);"
			+ "if (top.@srcRight != null)"
			+ "working.add(top.@srcRight);"
			+ "}"
			+ "for (@srcBin node : nodes.keySet()){"
			+ "@tgtBin bt = nodes.get(node);"
			+ "bt.@tgtLeft = nodes.get(node.@srcLeft);"
			+ "bt.@tgtRight = nodes.get(node.@srcRight);"
			+ "}"
			+ "@lhs = nodes.get(@arg);"
			+ "}";

	public Statement(int i, String n, String ret, List<String> types, List<Hole> holes) {
		id = i;
		name = n;
		retType = ret;
		argTypes = types;
		argHoles = holes;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String getRetType() {
		return retType;
	}

	public void setRetType(String retType) {
		this.retType = retType;
	}

	public List<Hole> getArgHoles() {
		return argHoles;
	}

	public void setArgHoles(List<Hole> argHoles) {
		this.argHoles = argHoles;
	}

	public List<String> getArgTypes() {
		return argTypes;
	}

	public void setArgTypes(List<String> argTypes) {
		this.argTypes = argTypes;
	}

	public DefVar getLhs() {
		return lhs;
	}

	public void setLhs(DefVar lhs) {
		this.lhs = lhs;
	}
	
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}
	
	public void setSootMethod(String m) {
		meth = m;
	}
	
	public boolean isArg() {
		return isArg;
	}

	public void setArg(boolean isArg) {
		this.isArg = isArg;
	}

	// code generation for each statement.
	public String toString() {
		/// check lltransition first.
		if (SootUtil.llTransitions.containsKey(name)) {
			Pent<String, String, String,String, String> trio = SootUtil.llTransitions.get(name);
			String src = trio.val0;
			String tgt = trio.val1;
			String srcObj = trio.val2;
			String tgtObj = trio.val3;
			String val = trio.val4;

			assert argHoles.size() == 1;
			String snippet = template.replace("@src", src).replace("@tgt", tgt).replace("@val", val)
					.replace("@lhs", lhs.getName()).replace("@arg", argHoles.get(0).getVar().toString())
					.replace("@next1", srcObj).replace("@next2", tgtObj).replace("@cnt", String.valueOf(cnt));
			cnt++;
			return snippet;
		}
		
		if (SootUtil.BinTransitions.containsKey(name)) {
			Pair<BinTree, BinTree> trio = SootUtil.BinTransitions.get(name);
			BinTree srcBin = trio.val0;
			BinTree tgtBin = trio.val1;

			assert argHoles.size() == 1;
			String snippet = binTemplate.replace("@srcBin", srcBin.getType()).replace("@tgtBin", tgtBin.getType())
					.replace("@srcId", srcBin.getId()).replace("@lhs", lhs.getName())
					.replace("@arg", argHoles.get(0).getVar().toString()).replace("@srcLeft", srcBin.getLeft())
					.replace("@srcRight", srcBin.getRight()).replace("@tgtLeft", tgtBin.getLeft())
					.replace("@tgtRight", tgtBin.getRight());
			return snippet;
		}
		
		//handle SDK
		if(name.contains("_sdk")) {
			String snippet = name.split("_sdk")[0];
			snippet = snippet.replace("?", argHoles.get(0).getVar().toString());
			snippet = retType + " " + getLhs().getName() + " = " + snippet + ";";
			return snippet;
		}
		
		//upcast
		if(name.contains("_upper")) {
			String snippet = argHoles.get(0).getVar().toString();
			snippet = retType + " " + getLhs().getName() + " = " + snippet + ";";
			return snippet;
		}

		StringBuilder sb = new StringBuilder();

		if (name.equals("return")) {
			sb.append(name).append(" ");
			assert argHoles.size() == 1;
			sb.append(argHoles.get(0).getVar()).append(";");
			return sb.toString();
		}
		if (!retType.equals("void")) {
			sb.append(retType).append(" ").append(lhs);
			if ("".equals(name)) {
				// declaration
				sb.append(";");
				return sb.toString();
			} else {
				sb.append(" = ");
			}
		}

		if (name.startsWith("new ")) {
			// constructor.
			sb.append(name).append("(");
			for (int i = 0; i < argHoles.size(); i++) {
				DefVar v = argHoles.get(i).getVar();
				sb.append(v);
				sb.append(",");
			}

			if (sb.lastIndexOf(",") > 0)
				sb.deleteCharAt(sb.lastIndexOf(","));

			sb.append(");");
			return sb.toString();
		}

		// downcast.
		if (name.startsWith("(")) {
			assert argHoles.size() == 1;
			DefVar v = argHoles.get(0).getVar();
			sb.append(name).append(v).append(";");
			return sb.toString();
		}

		if (isStatic) {
			sb.append(clazz).append(".").append(name).append("(");
			for (int i = 0; i < argHoles.size(); i++) {
				DefVar v = argHoles.get(i).getVar();
				sb.append(v);
				sb.append(",");
			}
			if (sb.lastIndexOf(",") > 0)
				sb.deleteCharAt(sb.lastIndexOf(","));

			sb.append(");");
		} else {
			if (argHoles.size() == 0) {
				sb.append(name).append("(");
			}
			for (int i = 0; i < argHoles.size(); i++) {
				DefVar v = argHoles.get(i).getVar();
				if (i == 0) {
					sb.append(v).append(".").append(name).append("(");
				} else {
					sb.append(v);
					sb.append(",");
				}
			}
			if (sb.lastIndexOf(",") > 0)
				sb.deleteCharAt(sb.lastIndexOf(","));

			sb.append(");");
		}

		return sb.toString();
	}
}
