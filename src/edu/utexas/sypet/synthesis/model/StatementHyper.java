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

public class StatementHyper {
	// name of the callee.
	private String name;

	// is static method
	private boolean isStatic = false;

	// return type.
	private String retType;

	// Holes list.
	private List<String> argHoles;

	// Type of argument list.
	private List<String> argTypes;

	// Variable in the lhs. Optional.
	private DefVar lhs;
	
	//declaring class
	private String clazz;
	
	
	private boolean isArg = false;
	
	public StatementHyper(String n, String ret, List<String> types, List<String> holes) {
		name = n;
		retType = ret;
		argTypes = types;
		argHoles = holes;
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

	public List<String> getArgHoles() {
		return argHoles;
	}

	public void setArgHoles(List<String> argHoles) {
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
	
	public boolean isArg() {
		return isArg;
	}

	public void setArg(boolean isArg) {
		this.isArg = isArg;
	}

	// code generation for each statement.
	public String toString() {
		//handle SDK
		if(name.contains("_sdk")) {
			String snippet = name.split("_sdk")[0];
			snippet = snippet.replace("?", argHoles.get(0));
			snippet = retType + " " + getLhs().getName() + " = " + snippet + ";";
			return snippet;
		}
		
		//upcast
		if(name.contains("_upper")) {
			String snippet = argHoles.get(0);
			snippet = retType + " " + getLhs().getName() + " = " + snippet + ";";
			return snippet;
		}

		StringBuilder sb = new StringBuilder();
		
		if (name.contains("dummy#")) {
			assert argHoles.size() == 1;
			sb.append(retType).append(" ").append(lhs).append(" = ").append(argHoles.get(0)).append(";");
			return sb.toString();
		}

		if (name.equals("return")) {
			sb.append(name).append(" ");
			assert argHoles.size() == 1;
			sb.append(argHoles.get(0)).append(";");
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
				String v = argHoles.get(i);
				sb.append(v);
				sb.append(",");
			}

			if (sb.lastIndexOf(",") > 0)
				sb.deleteCharAt(sb.lastIndexOf(","));

			sb.append(");");
			return sb.toString();
		}

		if (isStatic) {
			sb.append(clazz).append(".").append(name).append("(");
			for (int i = 0; i < argHoles.size(); i++) {
				String v = argHoles.get(i);
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
				String v = argHoles.get(i);
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
			
			if (retType.equals("void") && (argHoles.size() > 0)) {
				assert argTypes.size() > 0;
				String declType = argTypes.get(0);
				String rhs = argHoles.get(0);
				sb.append(declType + " " + lhs + " = " + rhs + ";");
			}
		}

		return sb.toString();
	}
}
