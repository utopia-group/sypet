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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import edu.utexas.hunter.model.CustomField;
import edu.utexas.hunter.model.CustomMethod;
import edu.utexas.hunter.model.CustomType;
import edu.utexas.sypet.Experiment;
import edu.utexas.sypet.synthesis.SyPetService;
import edu.utexas.sypet.synthesis.model.BinTree;
import edu.utexas.sypet.synthesis.model.Config;
import edu.utexas.sypet.synthesis.model.JGraph;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.model.Pent;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

public class SootUtil {

	// clone edges everywhere?
	protected static boolean clone = true;

	// Count the number of tokens? By default yes.
	protected static boolean countRes = true;

	public static Set<String> reachableTypes = new LinkedHashSet<>();

	public static Map<String, String> polyMap = new HashMap<>();

	protected static final JGraph graph = new JGraph();

	protected static Map<SootMethod, Set<String>> depMap = new HashMap<>();

	protected static Map<String, CustomMethod> hunterMap = new HashMap<>();

	protected static Map<String, Map<String, Integer>> consumeMap = new HashMap<>();

	// key: llTransition; Trio(s,t,f): s: src type, t: target type. f: intField
	public static Map<String, Pent<String, String, String, String, String>> llTransitions = new HashMap<>();

	public static Map<String, Pair<BinTree, BinTree>> BinTransitions = new HashMap<>();

	public static int classNum = 0;

	public static int methodNum = 0;

	private static final String UPPER = "_upper";

	private static Config cfg;

	public static void initCfg() {
		try {
			JsonReader reader = new JsonReader(new FileReader("CONFIG.json"));
			Gson gson = new Gson();
			cfg = gson.fromJson(reader, Config.class);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// summarize # of tokens for each type in the argument.
	public static Map<String, Integer> sumTokens4Type(List<?> list) {
		Map<String, Integer> map = new HashMap<>();
		for (Object t : list) {
			String name = t.toString();
			if (map.containsKey(name)) {
				int cnt = map.get(name);
				cnt++;
				map.put(name, cnt);
			} else {
				map.put(name, 1);
			}
		}
		return map;
	}

	// Given a method's signature, return # of tokens per each argument type.
	public static Map<String, Integer> getArgConsumeById(String sig) {

		if (consumeMap.containsKey(sig))
			return consumeMap.get(sig);
		else
			return null;
	}

	public static List<String> getClones(List<String> list) {
		Map<String, Integer> map = new HashMap<>();
		List<String> clones = new ArrayList<>();
		for (String name : list) {
			if (map.containsKey(name)) {
				int cnt = map.get(name);
				cnt++;
				map.put(name, cnt);
			} else {
				map.put(name, 1);
			}
		}
		for (String type : map.keySet()) {
			for (int j = 1; j < map.get(type); j++) {
				clones.add("sypet_clone_" + type);
			}
		}
		return clones;
	}

	public static boolean isValidMeth(SootMethod meth) {
		if (cfg == null)
			initCfg();

		List<String> blacklist = cfg.getBlacklist();
		for (String black : blacklist) {
			if (meth.getSignature().contains(black)) {
				return false;
			}
		}

		if (meth.getParameterCount() > 4)
			return false;

		if (!meth.isConstructor() && !meth.isPublic())
			return false;

		return true;
	}

	public static void createPlace(PetriNet p, String pName) {
		if (!p.containsPlace(pName)) {
			p.createPlace(pName);
			// clone edges
			if (clone) {
				String cloneId = "sypet_clone_" + pName;
				createTransition(p, cloneId);
				p.createFlow(pName, cloneId);
				p.createFlow(cloneId, pName, 2);
			}
		}
	}

	public static void createTransition(PetriNet p, String pName) {
		if (!p.containsTransition(pName)) {
			p.createTransition(pName);
		}
	}

	// Checking the class is linkedlist.
	public static boolean isLinkedlist(SootClass clz) {
		int intCnt = 0;
		int self = 0;
		for (SootField sf : clz.getFields()) {
			Type t = sf.getType();
			if (t.toString().equals("int"))
				intCnt++;

			if (t.toString().equals(clz.getName()))
				self++;
		}
		return ((intCnt == 1) && (self == 1));
	}

	public static boolean isLinkedlist(CustomType clz) {
		int intCnt = 0;
		int self = 0;
		for (CustomField sf : clz.getFields()) {
			String t = sf.getType();
			if (t.toString().equals("int"))
				intCnt++;

			if (t.toString().equals(clz.getName()))
				self++;
		}
		return ((intCnt == 1) && (self == 1));
	}

	public static boolean isBinaryTree(CustomType clz) {
		int intCnt = 0;
		int self = 0;
		for (CustomField sf : clz.getFields()) {
			String t = sf.getType();
			if (t.toString().equals("int"))
				intCnt++;

			if (t.toString().equals(clz.getName()))
				self++;
		}
		return ((intCnt == 1) && (self == 2));
	}

	// create dummy pair-wise transitions.
	public static void createLinkedlistTransition(PetriNet p, Set<SootClass> classes) {

		for (SootClass src : classes) {
			for (SootClass tgt : classes) {
				if (!src.equals(tgt)) {
					createPlace(p, src.toString());
					createPlace(p, tgt.toString());
					String name = src.toString() + "_" + tgt.toString();
					createTransition(p, name);
					String intField = getIntField(src);
					String objField = getObjField(src);
					String objTgtField = getObjField(tgt);

					Pent<String, String, String, String, String> trio = new Pent<>(src.toString(), tgt.toString(),
							objField, objTgtField, intField);
					llTransitions.put(name, trio);
					p.createFlow(src.toString(), name);
					p.createFlow(name, tgt.toString());
				}
			}
		}
	}

	public static void createLinkedlistTransition(PetriNet p, List<CustomType> classes) {
		if (classes.size() < 2)
			return;

		for (CustomType src : classes) {
			for (CustomType tgt : classes) {
				if (!src.equals(tgt)) {
					String name = src.getName() + "_" + tgt.getName();
					if (p.containsTransition(name))
						continue;
					createPlace(p, src.getName());
					createPlace(p, tgt.getName());

					createTransition(p, name);
					String intField = getIntField(src);
					String objField = getObjField(src);
					String objTgtField = getObjField(tgt);

					Pent<String, String, String, String, String> trio = new Pent<>(src.getName(), tgt.getName(),
							objField, objTgtField, intField);
					llTransitions.put(name, trio);
					p.createFlow(src.getName(), name);
					p.createFlow(name, tgt.getName());
				}
			}
		}
	}

	public static void createBinTransition(PetriNet p, List<CustomType> classes) {
		if (classes.size() < 2)
			return;

		for (CustomType src : classes) {
			String idSrc = getIntField(src);
			Pair<String, String> nodesSrc = getlfNodes(src);
			BinTree binSrc = new BinTree(idSrc, nodesSrc.val0, nodesSrc.val1, src.getName());
			createPlace(p, src.getName());
			for (CustomType tgt : classes) {
				if (!src.equals(tgt)) {
					String name = src.getName() + "_" + tgt.getName();
					if (p.containsTransition(name))
						continue;
					createPlace(p, tgt.getName());
					createTransition(p, name);
					String idTgt = getIntField(tgt);
					Pair<String, String> nodesTgt = getlfNodes(tgt);
					BinTree binTgt = new BinTree(idTgt, nodesTgt.val0, nodesTgt.val1, tgt.getName());
					Pair<BinTree, BinTree> trio = new Pair<>(binSrc, binTgt);
					BinTransitions.put(name, trio);
					p.createFlow(src.getName(), name);
					p.createFlow(name, tgt.getName());
				}
			}
		}
	}

	public static String getIntField(SootClass sc) {
		String str = "";
		for (SootField sf : sc.getFields()) {
			Type t = sf.getType();
			if (t.toString().equals("int")) {
				str = sf.getName();
				break;
			}
		}
		return str;
	}

	public static String getObjField(SootClass sc) {
		String str = "";
		for (SootField sf : sc.getFields()) {
			Type t = sf.getType();
			if (sc.getName().equals(t.toString())) {
				str = sf.getName();
				break;
			}
		}
		return str;
	}

	public static String getObjField(CustomType sc) {
		String str = "";
		for (CustomField sf : sc.getFields()) {
			String t = sf.getType();
			if (sc.getName().equals(t.toString())) {
				str = sf.getName();
				break;
			}
		}
		return str;
	}

	// get left & right nodes of a binary tree.
	public static Pair<String, String> getlfNodes(CustomType sc) {
		List<String> children = new ArrayList<>();
		for (CustomField sf : sc.getFields()) {
			String t = sf.getType();
			if (sc.getName().equals(t.toString())) {
				children.add(sf.getName());
			}
		}
		// order left/right by alphabetical order.
		java.util.Collections.sort(children);
		assert children.size() == 2;
		String v0 = children.get(0);
		String v1 = children.get(1);
		return new Pair<>(v0, v1);
	}

	public static String getIntField(CustomType sc) {
		String str = "";
		for (CustomField sf : sc.getFields()) {
			String t = sf.getType();
			if (t.toString().equals("int")) {
				str = sf.getName();
				break;
			}
		}
		return str;
	}

	// package of certain jar.
	public static void processJar(String jarPath, Set<String> pkg, PetriNet p) {
		pkg.addAll(getBuildinPkg());
		createPlace(p, "void");
		Set<SootClass> linkedlists = new HashSet<>();
		for (String cl : SourceLocator.v().getClassesUnder(jarPath)) {
			SootClass clazz = Scene.v().getSootClass(cl);
			boolean skip = true;
			for (String pName : pkg) {
				if (cl.startsWith(pName)) {
					skip = false;
					break;
				}
			}

			if (skip)
				continue;

			classNum++;
			if (isLinkedlist(clazz)) {
				linkedlists.add(clazz);
			}

			LinkedList<SootMethod> methodsCopy = new LinkedList<SootMethod>(clazz.getMethods());
			for (SootMethod meth : methodsCopy) {

				methodNum++;
				if (!isValidMeth(meth))
					continue;

				if (meth.isPublic() || meth.isStatic()) {

					String signature = meth.getSignature();
					String retName = meth.getReturnType().toString();

					boolean redirect = redirectFlow(cl, meth, retName, p);
					if (redirect)
						continue;

					LinkedList<Type> ll = new LinkedList<>();

					if (!meth.isStatic() && !meth.isConstructor()) {
						ll.add(clazz.getType());
					}
					ll.addAll(meth.getParameterTypes());

					createTransition(p, signature);
					createPlace(p, retName);
					Set<String> inputTypes = new LinkedHashSet<>();

					for (Type t : ll) {
						String pname = t.toString();
						// pname = substitute(pname, signature);
						inputTypes.add(pname);
						createPlace(p, pname);
					}

					// add flows.
					Map<String, Integer> map = sumTokens4Type(ll);
					consumeMap.put(signature, map);
					// arguments.
					for (String type : map.keySet()) {
						int cnt = countRes ? map.get(type) : 1;
						// type = substitute(type, signature);
						p.createFlow(type, signature, cnt);
					}
					// return.
					if (meth.isConstructor()) {
						String clzName = clazz.getName();
						createPlace(p, clzName);
						retName = clzName;
					}
					p.createFlow(signature, retName);
				}
			}

		}

		createLinkedlistTransition(p, linkedlists);
	}

	public static void updateReachableTypes(String tgtType, int val) {
		// set bound
		graph.setK(val);
		reachableTypes = graph.backwardReach2(tgtType);
	}

	// modify flow dues to inheritance.
	public static boolean redirectFlow(String cl, SootMethod meth, String retName, PetriNet p) {
		if (meth.isStatic() && meth.getParameterCount() == 0) {
			// from void to type.
			createTransition(p, meth.getSignature());
			createPlace(p, "void");
			createPlace(p, retName);
			p.createFlow("void", meth.getSignature());
			p.createFlow(meth.getSignature(), retName);
			return true;
		}

		// handle empty constructor.
		if (meth.isConstructor() && (meth.getParameterCount() == 0)) {
			// from void to type.
			createTransition(p, meth.getSignature());
			createPlace(p, "void");
			createPlace(p, cl);
			p.createFlow("void", meth.getSignature());
			p.createFlow(meth.getSignature(), cl);
			return true;
		}

		return false;
	}

	public static boolean isHunterMethod(String m) {
		return hunterMap.containsKey(m);
	}

	public static CustomMethod getHunterMethod(String m) {
		return hunterMap.get(m);
	}

	public static CustomMethod addHunterMethod(String m, CustomMethod meth) {
		return hunterMap.put(m, meth);
	}

	public static void setClone(boolean flag) {
		clone = flag;
	}

	public static void setCount(boolean flag) {
		countRes = flag;
	}

	// Generate compact PetriNet for Ruben.
	public static PetriNet getCompactGraph(PetriNet srcNet) {
		// /Compute max of each type.
		// init map.
		Map<String, Integer> maxTokenMap = new HashMap<>();
		for (Place p : srcNet.getPlaces()) {
			maxTokenMap.put(p.getId(), 1);
		}
		// compute max.
		for (Transition t : srcNet.getTransitions()) {
			for (Flow flow : t.getPresetEdges()) {
				String argType = flow.getSource().getId();
				int num = flow.getWeight();
				assert maxTokenMap.containsKey(argType);
				int curr = maxTokenMap.get(argType);
				if (num > curr)
					maxTokenMap.put(argType, num);
			}
		}

		// construct new PetriNet.
		PetriNet tgtNet = new PetriNet();
		// Create place first.
		for (String type : maxTokenMap.keySet()) {
			int num = maxTokenMap.get(type);
			List<String> cloneArgs = new ArrayList<>();
			for (int i = 0; i < num; i++) {
				String typeClone = type + "_" + i;
				tgtNet.createPlace(typeClone);
				cloneArgs.add(typeClone);
			}
			// Add reuse transition.
			int reuseCnt = 0;
			for (String src : cloneArgs) {
				for (String tgt : cloneArgs) {
					if (src.equals(tgt))
						continue;

					String reuse = "reuse_" + type + "_" + reuseCnt;
					tgtNet.createTransition(reuse);
					tgtNet.createFlow(src, reuse);
					tgtNet.createFlow(reuse, tgt);
					reuseCnt++;
				}
			}
		}

		// Create transition and flow
		for (Transition t : srcNet.getTransitions()) {
			assert t.getPostset().size() == 1;
			Place ret = t.getPostset().iterator().next();
			String retStr = ret.getId();
			// / clone this transition based on the # of clone.
			int cloneNum = maxTokenMap.get(retStr);
			for (int i = 0; i < cloneNum; i++) {
				String transitionClone = t.getId() + "_" + i;
				String retClone = retStr + "_" + i;
				// outgoing edge.
				tgtNet.createTransition(transitionClone);
				tgtNet.createFlow(transitionClone, retClone);
				// incoming edges.
				for (Flow flow : t.getPresetEdges()) {
					String argType = flow.getSource().getId();
					int tokens = flow.getWeight();
					for (int idx = 0; idx < tokens; idx++) {
						String argClone = argType + "_" + idx;
						assert tgtNet.containsPlace(argClone) : argClone;
						tgtNet.createFlow(argClone, transitionClone);
					}
				}
			}
		}

		return tgtNet;
	}

	public static void handlePolymorphism(PetriNet p) {

		assert cfg != null;
		List<String> polyList = cfg.getPoly();
		int upperCnt = 0;
		for (String raw : polyList) {
			// left is the subclass of rt. i.e., rt <= left.
			String left = raw.split(",")[0];
			String rt = raw.split(",")[1];
			if (!p.containsNode(left))
				createPlace(p, left);
			String tranName = UPPER + upperCnt;
			createTransition(p, tranName);
			if (!p.containsNode(rt))
				createPlace(p, rt);

			p.createFlow(left, tranName);
			p.createFlow(tranName, rt);
			SyPetService.sdkTypes.put(tranName, new Pair<>(left, rt));
			upperCnt++;
		}
	}

	public static Set<String> getBuildinPkg() {
		if (cfg == null)
			initCfg();

		Set<String> mySet = new HashSet<>(cfg.getBuildinPkg());
		return mySet;
	}
}
