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
package edu.utexas.sypet.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.utexas.hunter.model.DbItem;
import edu.utexas.sypet.synthesis.model.Pair;

public class SynthUtil {

	public static final String[] CONTAINERS = { "List", "Set", "Map", "Vector", "java.util.Vector", "java.util.Set",
			"java.util.List", "java.util.Map" };

	public static String[] buildinTypes = { "int", "short", "char", "long", "double", "String", "List", "Map", "Set",
	"Vector", "int[][]" };
	
	public static String[] numericType = { "byte", "int", "short", "char", "long", "float", "double"};
	
	/**
	 * Given a DbItem, return its actual callee(static/virtual).
	 * 
	 * @return
	 */
	public static String getCallee(DbItem item) {
		String clzName = item.getClassName();
		String meth = item.getName();
		StringBuilder sb = new StringBuilder();
		if (item.isStatic()) {
			sb.append(clzName);
		} else {
			// virtual.
			sb.append("new ").append(clzName).append("()");
		}
		sb.append(".").append(meth);
		return sb.toString();
	}
	
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static LinkedList<String> unpackCompositeType(String type) {
		LinkedList<String> list = new LinkedList<>();
		for (String s : type.split("<")) {
			if (!isContainer(s)) {
				/// remove additional '>' first.
				int end = s.indexOf('>');
				if (end != -1) {
					s = s.substring(0, end);
				}
				s = unbox(s);

				// is s an array?
				if (s.contains("[]")) {
					Pattern pattern = Pattern.compile("\\[\\]");
					Matcher matcher = pattern.matcher(s);
					while (matcher.find()) {
						list.add("Array");
					}
					s = s.replaceAll("\\[\\]", "");
				}
				list.add(s);
				break;
			}
			list.add(s);
		}
		return list;
	}

	public static boolean isContainer(String s) {
		return Arrays.asList(CONTAINERS).contains(s);
	}

	public static boolean appendAddAll(String s) {
		//FIXME!!!LCS V.S. Bresenham circle
		if (s.contains("Point"))
			return false;
		if (s.contains("List") || s.contains("Set") || s.contains("Vector"))
			return true;
		else
			return false;
	}

	public static String getNewStmt(String type, String elem) {
		String s = "N/A";
		if (type.equals("List") || type.equals("java.util.List")) {
			s = "new java.util.ArrayList<" + elem + ">();";
		} else if (type.equals("Set") || type.equals("java.util.Set")) {
			s = "new java.util.HashSet<" + elem + ">();";
		} else if (type.equals("Vector") || type.equals("java.util.Vector")) {
			s = "new java.util.Vector<" + elem + ">();";
		} else if (type.equals("Array")) {
			s = "new " + elem + "[100];";
		}

		// TODO: fix this ugly code.
		if (!type.equals("Array")) {
			s = s.replace("int", "Integer");
			s = s.replace("double", "Double");
			s = s.replace("long", "Long");
			s = s.replace("char", "Char");
			s = s.replace("short", "Short");
			s = s.replace("float", "Float");
			s = s.replace("byte", "Byte");
		}
		return s;
	}
	
	public static String box(String s) {
		// TODO: fix this ugly code.
		if (s.endsWith(">")) {
			s = s.replace("int", "Integer");
			s = s.replace("double", "Double");
			s = s.replace("long", "Long");
			s = s.replace("char", "Char");
			s = s.replace("short", "Short");
			s = s.replace("float", "Float");
			s = s.replace("byte", "Byte");
		} 
		
		s.replace("[][]", "[]");
		return s;
	}

	/**
	 * Checking whether s1 can be upcasted to s2.
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean compatible(String s1, String s2) {
		if (s1.equals(s2))
			return true;

		// FIXME: Quick hacking, will change to reachable graph later.
		if (s1.equals("int") && s2.equals("double"))
			return true;

		return false;
	}

	public static boolean equal(final Object x, final Object y) {
		return x == null ? y == null : x.equals(y);
	}

	public static String actualType(String str) {
		if (str.startsWith("List") || str.startsWith("Set") || str.startsWith("Vector"))
			str = str.replace("List", "java.util.List").replace("Set", "java.util.Set").replace("Vector",
					"java.util.Vector");
		if (!str.contains("Point") && !str.contains("String"))
			str = str.replaceAll("<[^>]*>", "");
		return str;
	}

	public static boolean isBuildin(String t) {
		return Arrays.asList(buildinTypes).contains(t) || Arrays.asList(SynthUtil.CONTAINERS).contains(t);
	}
	
	public static boolean isNum(String t) {
		return Arrays.asList(numericType).contains(t);
	}
	
	// return the default value of a specific type.
	public static String getDefaultVal(String type, boolean value) {
		if (Arrays.asList(numericType).contains(type)) {
			return "0";
		}

		if ("String".equals(type)) {
			return "\"\"";
		}
		
		if ("boolean".equals(type)) {
			if (value) return "true";
			else return "false";
		}

		if (Arrays.asList(CONTAINERS).contains(type)) {
			return "null";
		}
		assert false : "unknown: " + type;
		return "unknown";
	}
	
	// Integer -> int
	public static String unbox(String str) {
		String newStr = str.replace("java.lang.String", "String").replace("java.lang.Integer", "int")
				.replace("java.lang.Double", "double").replace("java.lang.Long", "long")
				.replace("java.lang.Char", "char").replace("java.lang.Short", "short")
				.replace("java.lang.Float", "float").replace("java.lang.Byte", "byte");

		return newStr;
	}
	
	public static String delPrefix(String s) {
		String prefix = "java.util.";
		if (s.startsWith(prefix) && !isContainer(s)) {
			return s.substring(prefix.length());
		} else {
			return s;
		}
	}
	
	public static String getLenOp(String s) {
		if (s.contains("[]")) {
			return ".length";
		} else {
			return ".size()";
		}
	}
	
	public static String genContainerType(String out, String in) {
		if (out.equals("Array")) {
			return in + "[]";
		} else {
			return out + "<" + in + ">";
		}
	}
	
	//decompose return stmt from the original stmt
	public static Pair<String, String> decompose(String stmt) {
		if (!stmt.contains(";")) {
			String st = "";
			if (stmt.equals("UNSAT"))
				st = stmt;
			return new Pair<>(st, stmt);
		}
		assert stmt.contains("return");
		String s1 = stmt.split("return")[0];
		String s2 = stmt.split("return")[1];
		String retVar = s2.replaceAll("\\s|;","");
		Pair<String, String> p = new Pair<>(s1, retVar);
		
		return p;
	}
	
	public static boolean customType(String s1, String s2) {
		return (!isBuildin(s1) || !isBuildin(s2));
	}
}
