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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import edu.utexas.hunter.model.CustomMethod;
import edu.utexas.hunter.model.CustomType;
import edu.utexas.sypet.SypetEnv;
import edu.utexas.sypet.synthesis.model.JEdge;
import edu.utexas.sypet.synthesis.model.JGraph;
import edu.utexas.sypet.synthesis.model.JNode;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.util.SootUtil;
import edu.utexas.sypet.util.SynthUtil;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

public class SyPetService {

	private PetriNet pNet = new PetriNet();

	private final String UNSAT = "UNSAT";

	private final int minLocal = 2;
	
	private final int maxLocal = 7;

	private final int maxTokens = 8;

	private PathFinder pf;

	private Sketcher sk;

	private boolean hasPath = false;

	private boolean hasSketch = false;

	private boolean hasInit = false;

	private List<Pair<String, String>> srcPairs;

	private String tgtType;
	
	private static final String UPPER = "_upper";
	
	private static final String SDK = "_sdk";
	
	public static Map<String, Pair<String,String>> sdkTypes = new HashMap<>();

	
	// default type conversion graph.
	public static final String JDKGRAPH = "edu/utexas/hunter/synthesis/JDK.json";

	public SyPetService() {
		/// init basic petrinet
		// InputStream is = getClass().getClassLoader().getResourceAsStream(JDKGRAPH);
		InputStream is = getClass().getClassLoader().getResourceAsStream("JDK.json");
		Gson gson = new Gson();
		BufferedReader br;
		
		br = new BufferedReader(new InputStreamReader(is));
		JGraph graph = gson.fromJson(br, JGraph.class);
		graph.buildDependency();

		// handle uppercast.
		int upperCnt = 0;
		for (JNode node : graph.getNodes()) {
			Set<JNode> uppers = graph.upperCastSet(node);
			String srcName = node.getName();
			if (uppers.isEmpty())
				continue;

			if (!pNet.containsNode(srcName))
				pNet.createPlace(srcName);

			for (JNode tgt : uppers) {
				String tgtName = tgt.getName();
				String tranName = UPPER + upperCnt;
				pNet.createTransition(tranName);
				if (!pNet.containsNode(tgtName))
					pNet.createPlace(tgtName);

				pNet.createFlow(srcName, tranName);
				pNet.createFlow(tranName, tgtName);
				sdkTypes.put(tranName, new Pair<>(srcName,tgtName));
				upperCnt++;
			}
		}

		// handler build-in functions.
		for (JEdge e : graph.getEdges()) {
			String label = e.getLabel();
			String src = graph.getNode(e.getSource()).getName();
			String tgt = graph.getNode(e.getTarget()).getName();

			if ("?".equals(label))
				continue;

			String tranName = label + SDK + upperCnt;
			sdkTypes.put(tranName, new Pair<>(src,tgt));

			SootUtil.createPlace(pNet, src);
			SootUtil.createPlace(pNet, tgt);
			SootUtil.createTransition(pNet, tranName);

			pNet.createFlow(src, tranName);
			pNet.createFlow(tranName, tgt);
			upperCnt++;
		}
		
		// build-in jdk types.
		
		//void
		SootUtil.createPlace(pNet, "void");
	}

	private void initSyPet(List<Pair<String, String>> srcs, String tgt, int localMax) {
		List<Place> inits = new ArrayList<>();
		List<String> types = new ArrayList<>();
		srcPairs = srcs;
		tgtType = tgt;

		for (Pair<String, String> pair : srcs) {
			if(!pNet.containsPlace(pair.val0))
				pNet.createPlace(pair.val0);
			
			Place srcPlace = pNet.getPlace(pair.val0);
			types.add(pair.val0);
			inits.add(srcPlace);
		}

		// adding void to initial marking.
		inits.add(pNet.getPlace("void"));
		// Multiple args: check if we need put clone as initial constraints.
		List<String> clones = SootUtil.getClones(types);
		Place tgtPlace = pNet.getPlace(tgt);
		pf = new PathFinder(pNet, inits, tgtPlace, localMax, maxTokens, null, clones);
	}

	public String doQuery() {
		if(srcPairs == null) return "UNSAT";
		
		if(trivial(srcPairs.get(0).val0, tgtType)) {
			return "UNSAT";
		}

		assert!srcPairs.isEmpty();
		return doQuery(srcPairs, tgtType);
	}
	
	public boolean trivial(String src, String tgt) {
		return (SynthUtil.isNum(src) && SynthUtil.isNum(tgt));
	}

	public String doQuery(List<Pair<String, String>> srcs, String tgt) {
		if (SypetEnv.debug) {
			System.out.println("doQuery---------------------" + srcs + "------>" + tgt);
			System.out.println("hasInit " + hasInit);
			System.out.println("hasPath " + hasPath);
		}


		int localMax = minLocal;
		if (!hasInit) {
			if(!pNet.containsPlace(tgt)) return UNSAT;
			initSyPet(srcs, tgt, localMax);
			hasInit = true;
		}
		String snippet = UNSAT;

		while (true) {
			List<String> solution = new ArrayList<>();

			if (!hasPath) {
				List<String> res = pf.get();
				
				if(SypetEnv.debug)
					System.out.println("localmax: " + localMax + " res:" + res);
				
				if (res.isEmpty()) {
					if(localMax < maxLocal) {
						localMax++;
						initSyPet(srcs, tgt, localMax);
						continue;
					}
						
					reset();
					return UNSAT;
				}

				for (String meth : res) {
					if (meth.startsWith("sypet_clone_"))
						continue;

					solution.add(meth);
				}
				hasPath = true;
			}

			if (!hasSketch) {
				//System.out.println("solutions:" + solution);
				//upper cast?
				if((solution.size() == 1) && solution.get(0).contains("_upper"))
					return srcs.get(0).val1;
				
				sk = new Sketcher(solution, srcs, tgt);
				boolean flag = sk.initSketch();
				if(!flag) {
					//get another path.
					hasPath = false;
					continue;
				}
				hasSketch = true;
			}
			snippet = sk.fillHoles();
			if (UNSAT.equals(snippet)) {
				reset();
				continue;
			}
			break;
		}

		return snippet;
	}
	
	public PetriNet buildPetri(String srcType, List<CustomType> customTypeList, String tgtType)
			throws Throwable {
		if (customTypeList.isEmpty())
			return null;

		SootUtil.createPlace(pNet, "void");
		SootUtil.createPlace(pNet, srcType);
		SootUtil.createPlace(pNet, tgtType);
		if (SypetEnv.debug) {
			System.out.println("buildPetrinet---------------------");
			System.out.println("Construct Petrinet from: " + srcType + "->" + tgtType);
			System.out.println("src: " + srcType);
			System.out.println("tgt: " + tgtType);
		}
		
		List<CustomType> linkedListTypes = new ArrayList<>();
		List<CustomType> binTypes = new ArrayList<>();

		for (CustomType type : customTypeList) {
			if (SootUtil.isLinkedlist(type)) {
				linkedListTypes.add(type);
				continue;
			}
			if (SootUtil.isBinaryTree(type)) {
				binTypes.add(type);
				continue;
			}
			addToPetriNet(type);
		}
		SootUtil.createLinkedlistTransition(pNet, linkedListTypes);
		SootUtil.createBinTransition(pNet, binTypes);

//		System.out.println("-----------------------------------------------");
//		for(Transition t : pNet.getTransitions()) {
//			String id = t.getId();
//			if(id.contains("_upper") || id.contains("_sdk")) continue;
//			System.out.println("Trans: " + t);
//			System.out.println("in: " + t.getPresetEdges());
//			System.out.println("out: " + t.getPostsetEdges());
//		}
		return this.pNet;
	}
	
	public CustomType getInnerType(CustomType type) {
		CustomMethod meth = new CustomMethod();
		meth.setConstructor(false);
		meth.setDeclaredClass("org.eclipse.jetty.http.HttpStatus.Code");
		meth.setName("getMessage");
		meth.setParams(new ArrayList<String>());
		meth.setRetType("java.lang.String");
		meth.setSignature("<org.eclipse.jetty.http.HttpStatus.Code: java.lang.String getMessage()>");
		meth.setStaticMethod(false);
		type.getMethods().add(meth);
		type.setName("org.eclipse.jetty.http.HttpStatus.Code");
		return type;
	}
	
	public void addToPetriNet(CustomType type) {
		if("org.eclipse.jetty.http.Code".equals(type.getName()))
			type = getInnerType(type);
		
		SootUtil.createPlace(pNet, type.getName());
		for (CustomMethod meth : type.getMethods()) {
			String declClz = meth.getDeclaredClass();
			String api = meth.getSignature();
			if ("<org.eclipse.jetty.http.HttpStatus: org.eclipse.jetty.http.Code getCode(int)>".equals(api)) {
				api = "<org.eclipse.jetty.http.HttpStatus: org.eclipse.jetty.http.HttpStatus.Code getCode(int)>";
				meth.setRetType("org.eclipse.jetty.http.HttpStatus.Code");
			}
			if (pNet.containsTransition(api))
				continue;

			SootUtil.addHunterMethod(api, meth);
			SootUtil.createTransition(pNet, api);

			// constructor?
			if (meth.isConstructor() && meth.getParams().isEmpty()) {
				SootUtil.createPlace(pNet, declClz);
				pNet.createFlow("void", api);
				pNet.createFlow(api, declClz);
				continue;
			}

			List<String> params = new ArrayList<>();
			if (!meth.isConstructor() && !meth.isStaticMethod()) {
				// add receiver as the first param.
				params.add(declClz);
			}

			if (meth.isStaticMethod() && meth.getParams().isEmpty()) {
				params.add("void");
			}
			params.addAll(meth.getParams());
			assert !params.isEmpty();
			Map<String, Integer> map = SootUtil.sumTokens4Type(params);

			for (String param : map.keySet()) {
				Transition apiTran = pNet.getTransition(api);
				if (!apiTran.getPostsetEdges().isEmpty())
					continue;
				int num = map.get(param);
				assert num > 0;
				SootUtil.createPlace(pNet, param);
				pNet.createFlow(param, api, num);
			}
			String retType = meth.getRetType();
			if (meth.isConstructor())
				retType = declClz;
			SootUtil.createPlace(pNet, retType);
			pNet.createFlow(api, retType);
		}
	}
	
	public void setPetriNet(PetriNet net) {
		pNet = net;
	}

	public boolean isValid(String tgt) {
		return (pNet.getPlaces().isEmpty() || pNet.getTransitions().isEmpty() || !pNet.containsPlace(tgt));
	}
	
	public void printTrans() {
		for (Transition t : pNet.getTransitions()) {
			System.out.println("Transition***:" + t + " args:" + t.getPreset() + " ret:" + t.getPostset());
		}
		System.out.println(pNet.getPlaces());
	}

	public void reset() {
		srcPairs = null;
		hasPath = false;
		hasSketch = false;
		hasInit = false;
	}
}
