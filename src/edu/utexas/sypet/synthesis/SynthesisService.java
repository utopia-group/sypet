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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import edu.utexas.sypet.SypetEnv;
import edu.utexas.sypet.synthesis.model.DefVarFactory;
import edu.utexas.sypet.synthesis.model.JGraph;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.util.SynthUtil;

public class SynthesisService {

	private static final Logger LOG = Logger.getLogger(SynthesisService.class.getName());

	protected JGraph graph;

	public static final String INVALID = "N/A";

	public SyPetService syPet;

	public Pair<String, String> typeConversion(Pair<String, String> src, String tgtType) throws Throwable {
		if(SypetEnv.debug)
			System.out.println("new conversion: ====" + src + "---->" + tgtType);
		String lastStmt = INVALID;

		String srcType = src.val0;
		LinkedList<String> srcTypeList = SynthUtil.unpackCompositeType(srcType);
		String srcVar = src.val1;
		LinkedList<String> tgtTypeList = SynthUtil.unpackCompositeType(tgtType);
		if ((tgtTypeList.size() != srcTypeList.size()) || srcVar.equals("unknown")) {
			return new Pair<>("", lastStmt);
		} else {
			String srcLast = srcTypeList.getLast();
			String tgtLast = tgtTypeList.getLast();

			// generic
			if (tgtLast.equals("?") || tgtLast.equals("T")) {
				tgtLast = tgtLast.replace("?", srcLast);
				tgtLast = tgtLast.replace("T", srcLast);
				tgtTypeList.removeLast();
				tgtTypeList.addLast(tgtLast);
			}

			if (srcLast.equals("?") || srcLast.equals("T")) {
				srcLast = srcLast.replace("?", tgtLast);
				srcLast = srcLast.replace("T", tgtLast);
				srcTypeList.removeLast();
				srcTypeList.addLast(srcLast);
			}

			if (SypetEnv.debug) {
				System.out.println("srcList: " + srcTypeList);
				System.out.println("tgtList: " + tgtTypeList);
			}
			if (tgtTypeList.equals(srcTypeList)) {
				return new Pair<>("", srcVar);
			} else {
				// two types are not completely identical.
				// Then we have to do the deep copy.
				String tgtOuter = tgtTypeList.removeFirst();
				String srcOuter = srcTypeList.removeFirst();

				// reduce to base case.
				if (tgtTypeList.size() == 0) {
					return typeConversion(srcLast, srcVar, tgtLast);
				} else if (tgtLast.equals(srcLast) && (tgtTypeList.size() == 1) && !tgtOuter.equals("Array")) {
					// inner type is the same, do container transformation
					// directly.
					return typeConversion(srcOuter, srcVar, tgtOuter);
				} else if (srcTypeList.size() < 2) {
//					System.out.println("one dimension: " + srcTypeList + "---->" + tgtTypeList);
					// Worse case, generate loop structure.
					StringBuilder sb = new StringBuilder();
					String outVar = DefVarFactory.getHunterVar();
					String inVar = DefVarFactory.getHunterVar();

					// 1. new instance.
					String newStmt = SynthUtil.getNewStmt(tgtOuter, tgtLast);
					newStmt = newStmt.replace("100", srcVar + SynthUtil.getLenOp(srcType));
					sb.append(tgtType).append(" ").append(outVar).append(" = ").append(newStmt);
					// need for-loop(s).
					// The only special case is to perform store op on
					// array.
					// 2.1 loop header.
					String idxStr = DefVarFactory.getHunterVar();
					sb.append(" int ").append(idxStr).append(" = 0;");
					sb.append("for(").append(srcLast).append(" ").append(inVar).append(" : ").append(srcVar)
							.append("){");

					// 2.2 loop body.
					Pair<String, String> pair = typeConversion(srcLast, inVar, tgtLast);
					if(pair.val0 != null) sb.append(pair.val0);
					if (tgtOuter.equals("Array")) {
						sb.append(outVar).append("[").append(idxStr).append("] = ").append(pair.val1).append(";");
					} else {
						sb.append(outVar).append(".add(").append(pair.val1).append(");");
					}
					sb.append(idxStr).append("++;");
					// 2.3 loop end.
					sb.append("}");
					// 3. add to instance.
					return new Pair<>(sb.toString(), outVar);
				} else {
					// two dimension container.
					// 1. outer loop.
					StringBuilder sb = new StringBuilder();
					String outVar = DefVarFactory.getHunterVar();
					String outVar2 = DefVarFactory.getHunterVar();
					String inVar = DefVarFactory.getHunterVar();
					String inVar2 = DefVarFactory.getHunterVar();
					String idx1 = DefVarFactory.getHunterVar();
					String idx2 = DefVarFactory.getHunterVar();
					String outerNewStmt = SynthUtil.getNewStmt(tgtOuter, tgtLast);

					String innerType = SynthUtil.genContainerType(srcOuter, srcLast);
					innerType = SynthUtil.box(innerType);
					tgtType = SynthUtil.box(tgtType);
					String tgtInnerType = tgtType;
					if (!tgtTypeList.contains("Array")) {
						tgtInnerType = tgtTypeList.getFirst() + "<" + tgtTypeList.getLast() + ">";
					}
					if (!tgtType.contains("[]")) {
						outerNewStmt = "new " + tgtType + "();";
					}

					tgtInnerType = SynthUtil.box(tgtInnerType);
					srcType = SynthUtil.box(srcType);

					outerNewStmt = outerNewStmt.replace("100", srcVar + SynthUtil.getLenOp(srcType));

					String innerNewStmt = SynthUtil.getNewStmt(tgtOuter, tgtLast);
					innerNewStmt = innerNewStmt.replace("100", inVar + SynthUtil.getLenOp(srcType));
					if (!tgtType.contains("[]")) {
						innerNewStmt = "new " + tgtInnerType + "();";
					}
					// 1. new instance.
					sb.append(tgtType).append(" ").append(outVar).append(" = ").append(outerNewStmt);

					if (tgtOuter.contains("Array"))
						sb.insert(sb.length() - 1, "[]");
					// 2.1 loop header.
					sb.append(" int ").append(idx1).append(" = 0;");
					sb.append("for(").append(innerType);
					sb.append(" ");
					sb.append(inVar).append(" : ").append(srcVar).append("){");

					sb.append(" int ").append(idx2).append(" = 0;");
					sb.append(tgtInnerType.replace("[][]", "[]")).append(" ").append(outVar2).append(" = ")
							.append(innerNewStmt);
					sb.append("for(").append(srcLast).append(" ").append(inVar2).append(" : ").append(inVar)
							.append("){");
					Pair<String, String> pair = typeConversion(srcLast, inVar2, tgtLast);
					if(pair.val0 != null) sb.append(pair.val0);

					if (tgtTypeList.contains("Array"))
						sb.append(outVar2).append("[").append(idx2).append("] = ").append(pair.val1);
					else
						sb.append(outVar2).append(".add(").append(pair.val1).append(")");

					sb.append(";");
					sb.append(idx2).append("++;");
					sb.append("}");

					// 2.2 loop body.
					if (tgtTypeList.contains("Array"))
						sb.append(outVar).append("[").append(idx1).append("] = ").append(outVar2);
					else
						sb.append(outVar).append(".add(").append(outVar2).append(")");

					sb.append(";");
					sb.append(idx1).append("++;");
					sb.append("}");
					// 2. inner loop.
					return new Pair<>(sb.toString(), outVar);
				}

			}

		}
	}

	public Pair<String, String> typeConversion(Pair<String, String> src, Pair<String, String> tgt) throws Throwable {
		String lastStmt = INVALID;
		String srcType = src.val0;
		String srcVar = src.val1;
		String tgtType = tgt.val0;
		String tgtVar = tgt.val1;

		LinkedList<String> srcTypeList = SynthUtil.unpackCompositeType(srcType);
		LinkedList<String> tgtTypeList = SynthUtil.unpackCompositeType(tgtType);
		if (SypetEnv.debug) {
			System.out.println("new conversion pair: ====" + src + "---->" + tgt);
			System.out.println("srcList: " + srcTypeList);
			System.out.println("tgtList: " + tgtTypeList);
		}

		if (tgtTypeList.size() != srcTypeList.size()) {
			return new Pair<>("", lastStmt);
		} else {
			String srcLast = srcTypeList.getLast();
			String tgtLast = tgtTypeList.getLast();

			// generic
			if (tgtLast.equals("?") || tgtLast.equals("T")) {
				tgtLast = tgtLast.replace("?", srcLast);
				tgtLast = tgtLast.replace("T", srcLast);
				tgtTypeList.removeLast();
				tgtTypeList.addLast(tgtLast);
			}

			if (srcLast.equals("?") || srcLast.equals("T")) {
				srcLast = srcLast.replace("?", tgtLast);
				srcLast = srcLast.replace("T", tgtLast);
				srcTypeList.removeLast();
				srcTypeList.addLast(srcLast);
			}

			if (tgtTypeList.equals(srcTypeList)) {
				return new Pair<>("", srcVar);
			} else {
				// two types are not completely identical.
				// Then we have to do the deep copy.
				String tgtOuter = tgtTypeList.removeFirst();
				String srcOuter = srcTypeList.removeFirst();

				// reduce to base case.
				if (tgtTypeList.size() == 0) {
					return typeConversion(srcLast, srcVar, tgtLast);
				} else if (tgtLast.equals(srcLast) && (tgtTypeList.size() == 1)) {
					// inner type is the same, do container transformation
					// directly.
					return typeConversion(srcOuter, srcVar, tgtOuter);
				} else if (srcTypeList.size() < 2) {
					// Worse case, generate loop structure.
					StringBuilder sb = new StringBuilder();
					String outVar = tgtVar;
					String inVar = DefVarFactory.getHunterVar();

					// need for-loop(s).
					// The only special case is to perform store op on
					// array.
					// 2.1 loop header.
					String idxStr = DefVarFactory.getHunterVar();
					sb.append(" int ").append(idxStr).append(" = 0;");
					sb.append("for(").append(srcLast).append(" ").append(inVar).append(" : ").append(srcVar)
							.append("){");

					// 2.2 loop body.
					Pair<String, String> pair = typeConversion(srcLast, inVar, tgtLast);
					String snippet = pair.val0;
					if (snippet != null)
						sb.append(snippet);

					if (tgtOuter.equals("Array")) {
						sb.append(outVar).append("[").append(idxStr).append("] = ").append(pair.val1).append(";");
					} else {
						sb.append(outVar).append(".add(").append(pair.val1).append(");");
					}
					sb.append(idxStr).append("++;");
					// 2.3 loop end.
					sb.append("}");
					System.out.println("my new conversion: " + sb.toString());
					// 3. add to instance.
					return new Pair<>(sb.toString(), outVar);
				} else {
					// two dimension container.
					// 1. outer loop.
					StringBuilder sb = new StringBuilder();
					String outVar = tgtVar;
					String outVar2 = DefVarFactory.getHunterVar();
					String inVar = DefVarFactory.getHunterVar();
					String inVar2 = DefVarFactory.getHunterVar();
					String idx1 = DefVarFactory.getHunterVar();
					String idx2 = DefVarFactory.getHunterVar();
					// String outerNewStmt = SynthUtil.getNewStmt(tgtOuter,
					// tgtLast);

					String innerType = SynthUtil.genContainerType(srcOuter, srcLast);
					innerType = SynthUtil.box(innerType);
					tgtType = SynthUtil.box(tgtType);
					String tgtInnerType = tgtType;
					if (!tgtTypeList.contains("Array")) {
						tgtInnerType = tgtTypeList.getFirst() + "<" + tgtTypeList.getLast() + ">";
					}
					// if(!tgtType.contains("[]")) {
					// outerNewStmt = "new " + tgtType + "();";
					// }

					tgtInnerType = SynthUtil.box(tgtInnerType);
					srcType = SynthUtil.box(srcType);

					// outerNewStmt = outerNewStmt.replace("100", srcVar +
					// SynthUtil.getLenOp(srcType));

					String innerNewStmt = SynthUtil.getNewStmt(tgtOuter, tgtLast);
					innerNewStmt = innerNewStmt.replace("100", inVar + SynthUtil.getLenOp(srcType));
					if (!tgtType.contains("[]")) {
						innerNewStmt = "new " + tgtInnerType + "();";
					}
					// 1. new instance.
					// sb.append(tgtType).append(" ").append(outVar).append(" =
					// ").append(outerNewStmt);

					// if(tgtOuter.contains("Array")) sb.insert(sb.length() - 1,
					// "[]");
					// 2.1 loop header.
					sb.append(" int ").append(idx1).append(" = 0;");
					sb.append("for(").append(innerType);
					sb.append(" ");
					sb.append(inVar).append(" : ").append(srcVar).append("){");

					sb.append(" int ").append(idx2).append(" = 0;");
					sb.append(tgtInnerType.replace("[][]", "[]")).append(" ").append(outVar2).append(" = ")
							.append(innerNewStmt);
					sb.append("for(").append(srcLast).append(" ").append(inVar2).append(" : ").append(inVar)
							.append("){");
					Pair<String, String> pair = typeConversion(srcLast, inVar2, tgtLast);
					if (tgtTypeList.contains("Array"))
						sb.append(outVar2).append("[").append(idx2).append("] = ").append(pair.val1);
					else
						sb.append(outVar2).append(".add(").append(pair.val1).append(")");

					sb.append(";");
					sb.append(idx2).append("++;");
					sb.append("}");

					// 2.2 loop body.
					if (tgtTypeList.contains("Array"))
						sb.append(outVar).append("[").append(idx1).append("] = ").append(outVar2);
					else
						sb.append(outVar).append(".add(").append(outVar2).append(")");

					sb.append(";");
					sb.append(idx1).append("++;");
					sb.append("}");
					// 2. inner loop.
					return new Pair<>(sb.toString(), outVar);
				}
			}

		}
	}

	public String querySAT() {
		String res = syPet.doQuery();
		Pair<String, String> pair = SynthUtil.decompose(res);
		return pair.val0;
	}

	private String current = "";

	public String nextSolution(String oldCode, String newCode) throws Throwable {
		String newStr = oldCode.replace(current, newCode);
		if (SypetEnv.debug) {
			System.out.println("replace " + current + " by: " + newCode);
			System.out.println("contains old code: " + oldCode.contains(current));
		}
		current = newCode;
		return newStr;
	}

	public Pair<String, String> typeConversion(String srcType, String srcVar, String tgtType) throws Throwable {
		if(SypetEnv.debug)
			LOG.info(srcType + " [var] " + srcVar + " ===> " + tgtType);
		
		srcType = SynthUtil.delPrefix(srcType);
		String lastStmt = INVALID;

		if (srcType.equals(tgtType) || "T".equals(tgtType)) {
			lastStmt = srcVar;
			return new Pair<>("", lastStmt);
		}
		
//		syPet = new SyPetService();
		List<Pair<String, String>> srcs = new ArrayList<>();
		srcs.add(new Pair<>(srcType, srcVar));
		String res = syPet.doQuery(srcs, tgtType);
		if(res.equals("UNSAT"))
			return new Pair<>("", lastStmt);

		Pair<String, String> pair = SynthUtil.decompose(res);
		current = pair.val0;

		return pair;
	}
	
	public void print() {
		syPet.printTrans();
	}
	
}
