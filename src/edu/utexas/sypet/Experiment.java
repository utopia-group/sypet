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
package edu.utexas.sypet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import edu.utexas.sypet.synthesis.PathFinder;
import edu.utexas.sypet.synthesis.Sketcher;
import edu.utexas.sypet.synthesis.SypetTestUtil;
import edu.utexas.sypet.synthesis.model.Benchmark;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.sat4j.PetrinetEncoding.Option;
import edu.utexas.sypet.util.SootUtil;
import edu.utexas.sypet.util.TimeUtil;
import polyglot.ext.jl.types.PlaceHolder_c;
import soot.CompilationDeathException;
import soot.Scene;
import soot.options.Options;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

// Status: ?
/**
 * Parametric framework for running benchmarks
 *
 */
public class Experiment {
	public static boolean VERBOSE = false;
	public static String benchLoc = null;
	public static long TIMEOUT = 600000;

	public static final int maxTokens = 10;

	public static Option objectiveOption = Option.AT_LEAST_ONE;
	public static int maxIterations = 5;

	public static List<String> clones;

	public static PathFinder initPetriNet(Benchmark qb, List<PetriNet> pNetList, int pn, int local) {

		if (pNetList.size() == 1) {
			pn = 0;
		}

		PetriNet pNet = pNetList.get(pn);

		System.out.println("PetriNet for path length: " + local + " [places: " + pNet.getPlaces().size()
				+ " ; transitions: " + pNet.getTransitions().size() + " ; edges: " + pNet.getEdges().size() + "]");

		List<Place> inits = new ArrayList<>();
		List<Pair<String, String>> vars = new ArrayList<>();
		int index = 0;
		for (String src : qb.getSrcTypes()) {
			Place srcPlace = pNet.getPlace(src);
			inits.add(srcPlace);
			String varName = qb.getParamNames().get(index);
			Pair<String, String> arg = new Pair<>(src, varName);
			vars.add(arg);
			index++;
		}
		// adding void to initial marking.
		inits.add(pNet.getPlace("void"));
		// tgt place.
		String tgt = qb.getTgtType();
		Place tgtPlace = pNet.getPlace(tgt);

		PathFinder pf = new PathFinder(pNet, inits, tgtPlace, local, maxTokens, clones, objectiveOption, maxIterations);
		pf.setVars(vars);
		pf.setTgt(tgt);
		return pf;

	}

	public static void main(String[] args) throws FileNotFoundException {
		long startSoot = System.nanoTime();
		int roundRobinPosition = 0;
		int roundRobinIterations = 0;
		int roundRobinIterationsLimit = 40;
		int roundRobinRange = 3;
		boolean roundRobinFlag = true;

		Cli cmdOptions = new Cli(args);
		cmdOptions.parse();

		VERBOSE = cmdOptions.getVerbose();
		TIMEOUT = cmdOptions.getTimeout();
		roundRobinFlag = cmdOptions.getRoundRobin();
		// The objective function can be used to add preferences over which
		// methods to explore
		objectiveOption = objectiveOption.AT_LEAST_ONE;
		maxIterations = cmdOptions.getSolverLimit();

		cmdOptions.printOptions();

		benchLoc = cmdOptions.getFilename();
		if (!new File(benchLoc).exists()) {
			System.out.println("Cannot find json file: " + benchLoc);
			System.exit(2);
		}

		double timeGetPath = 0;
		double timeInitSketch = 0;
		double timeFillHoles = 0;
		double timeCompilation = 0;
		double timeRunTest = 0;
		double timeTotal = 0;
		long cntFillHoles = 0;
		Gson gson = new Gson();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(benchLoc));
			Benchmark qb = gson.fromJson(br, Benchmark.class);
			// generate the method header
			qb.setMethodHeader(genMethodHeader(qb));
			// generate the test string
			qb.setTestBody(genTest(qb));

			/////////////////////////////////////////////////////////////////////////////////
			System.out.println("----------" + benchLoc);
			System.out.println("Benchmark Id: " + qb.getId());
			System.out.println("Method name: " + qb.getMethodName());
			System.out.println("Packages: " + qb.getPackages());
			System.out.println("Libraries: " + qb.getLibs());
			System.out.println("Source type(s): " + qb.getSrcTypes());
			System.out.println("Target type: " + qb.getTgtType());
			System.out.println("--------------------------------------------------------");

			///////////////////////////////////////////////////
			Set<String> pkgs = qb.getPackages();
			String keyword = qb.getMethodName();

			StringBuilder options = new StringBuilder();
			options.append("-prepend-classpath");
			options.append(" -full-resolver");
			options.append(" -allow-phantom-refs");
			StringBuilder cp = new StringBuilder();
			for (String lib : qb.getLibs()) {
				cp.append(lib);
				cp.append(":");
				options.append(" -process-dir " + lib);
			}

			options.append(" -cp " + cp.toString());

			if (!Options.v().parse(options.toString().split(" ")))
				throw new CompilationDeathException(CompilationDeathException.COMPILATION_ABORTED,
						"Option parse error");

			Scene.v().loadBasicClasses();
			Scene.v().loadNecessaryClasses();

			List<PetriNet> pNetList = new ArrayList<>();

			// SootUtil.genDepGraph(qb.getLibs(), pkgs, qb.getTgtType());
			// FIXME: get lower bound. Will place with shortest path.
			// int low = Math.max(1, SootUtil.getLowerBound(qb));
			int low = 1;

			PetriNet pNet = new PetriNet();
			// only one petrinet without pruning.
			for (String lib : qb.getLibs()) {
				SootUtil.processJar(lib, pkgs, pNet);
			}
			SootUtil.handlePolymorphism(pNet);
			System.out.println("#Classes: " + SootUtil.classNum);
			System.out.println("#Methods: " + SootUtil.methodNum);
			long endSoot = System.nanoTime();
			double sootTime = TimeUtil.computeTime(startSoot, endSoot);
			System.out.println("Soot Time: " + sootTime);
			pNetList.add(pNet);

			// Multiple args: check if we need put clone as initial constraints.
			clones = SootUtil.getClones(qb.getSrcTypes());

			int petriIterator = 0;

			// System.out.println("Petri nets: " + pNetList.size());

			int cnt = 0;
			// int localMax = 1 + clones.size();
			// System.out.println("Lower bound: " + low);
			int localMax = low;
			boolean flag = false;
			long start0 = System.nanoTime();

			assert (roundRobinRange < 7);
			ArrayList<PathFinder> roundRobin = new ArrayList<>();
			if (roundRobinFlag) {
				for (int i = 0; i < roundRobinRange; i++) {
					roundRobin.add(initPetriNet(qb, pNetList, petriIterator++, localMax++));
				}
			}

			while (!flag) {
				PathFinder pf;
				if (roundRobinFlag) {
					pf = roundRobin.get(roundRobinPosition);
					// if (VERBOSE)
					// System.out.println("Searching with local max: " +
					// pf.getEncoding().getTimeline());
				} else {
					// pf = new PathFinder(pNet, inits, tgtPlace, localMax,
					// maxTokens, nlplist, clones,
					// objectiveOption, maxIterations);
					if (pNetList.size() > 1)
						assert (petriIterator < pNetList.size());
					pf = initPetriNet(qb, pNetList, petriIterator++, localMax);
				}

				if (VERBOSE && !roundRobinFlag)
					System.out.println("Searching with local max: " + localMax);
				while (roundRobinIterations < roundRobinIterationsLimit || !roundRobinFlag) {
					long start1 = System.nanoTime();
					List<String> res = pf.get();
					long end1 = System.nanoTime();
					timeGetPath += TimeUtil.computeTime(start1, end1);
					if (VERBOSE)
						TimeUtil.reportTime(start1, end1, "get path: ");
					if (VERBOSE)
						System.out.println("call SAT." + cnt + " val: " + res);

					if (res.isEmpty())
						break;

					cnt++;

					List<String> solution = new ArrayList<>();
					for (String meth : res) {
						if (meth.startsWith("sypet_clone_"))
							continue;

						solution.add(meth);
					}
					// System.out.println("current sketch:" + res);
					// init sketcher.
					long start2 = System.nanoTime();
					Sketcher sk = new Sketcher(solution, pf.getVars(), pf.getTgt());
					boolean hasSketch = sk.initSketch();
					long end2 = System.nanoTime();
					timeInitSketch += TimeUtil.computeTime(start2, end2);
					if (VERBOSE)
						System.out.println("#holes: " + sk.getHolesNum());
					if (VERBOSE)
						TimeUtil.reportTime(start2, end2, "init sketch: ");
					while (true) {
						++cntFillHoles;
						long start3 = System.nanoTime();
						String snippet = sk.fillHoles();
						long end3 = System.nanoTime();
						timeFillHoles += TimeUtil.computeTime(start3, end3);
						if (VERBOSE)
							TimeUtil.reportTime(start3, end3, "fill hole: ");
						if (snippet.equals("UNSAT"))
							break;

						// invoke yuepeng's method.
						if (VERBOSE)
							System.out.println("snippet:" + snippet);
						qb.setBody(snippet);
						boolean passTest = SypetTestUtil.runTest(qb);
						timeCompilation += SypetTestUtil.getCompilationTime();
						timeRunTest += SypetTestUtil.getRunningTime();
						if (VERBOSE)
							System.out.println("Test result-------------" + passTest);
						if (VERBOSE)
							System.out.println("Compilation Time: " + SypetTestUtil.getCompilationTime());
						if (VERBOSE)
							System.out.println("Running Time: " + SypetTestUtil.getRunningTime());
						long end0 = System.nanoTime();
						// note: this should be = instead of +=
						timeTotal = TimeUtil.computeTime(start0, end0);
						if (passTest) {
							System.out.println("=========Statistics (time in milliseconds)=========");
							System.out.println("Benchmark Id: " + qb.getId());
							System.out.println("Sketch Generation Time: " + timeGetPath);
							System.out.println("Sketch Completion Time: " + (timeInitSketch + timeFillHoles));
							System.out.println("Compilation Time: " + timeCompilation);
							System.out.println("Running Test cases Time: " + timeRunTest);
							System.out.println(
									"Synthesis Time: " + (timeGetPath + timeInitSketch + timeFillHoles + timeRunTest));
							System.out.println("Total Time: "
									+ (timeGetPath + timeInitSketch + timeFillHoles + timeRunTest + timeCompilation));
							System.out.println("Number of components: " + res.size());
							System.out.println("Number of holes: " + sk.getHolesNum());
							System.out.println("Number of completed programs: " + cntFillHoles);
							System.out.println("Number of sketches: " + cnt);
							System.out.println("Solution:\n " + snippet.replace(";", ";\n "));

							System.out.println("============================");
							br.close();
							return;
						} else if (timeTotal > TIMEOUT) {
							System.out.println("=========Statistics=========");
							System.out.println("Benchmark Id: " + qb.getId());
							System.out.println("Sketch Generation Time: " + timeGetPath);
							System.out.println("Sketch Completion Time: " + (timeInitSketch + timeFillHoles));
							System.out.println("Compilation Time: " + timeCompilation);
							System.out.println("Running Test cases Time: " + timeRunTest);
							System.out.println("Number of completed programs: " + cntFillHoles);
							System.out.println("Number of sketches: " + cnt);
							System.out.println("TIMEOUT after " + TIMEOUT + " ms");
							System.out.println("============================");
							br.close();
							return;
						}
					}
					roundRobinIterations++;
				}
				if (roundRobinFlag) {
					if (roundRobinIterations != roundRobinIterationsLimit) {
						if (pNetList.size() > 1)
							assert (petriIterator < pNetList.size());
						roundRobin.set(roundRobinPosition, initPetriNet(qb, pNetList, petriIterator++, localMax++));
					}
					roundRobinPosition = (roundRobinPosition + 1) % roundRobin.size();
					roundRobinIterations = 0;
				} else {
					localMax++;
				}
			}
		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected static String genMethodHeader(Benchmark bench) {
		StringBuilder builder = new StringBuilder();
		builder.append(bench.getTgtType().replaceAll("\\$", ".")).append(' ');
		builder.append(bench.getMethodName()).append('(');
		ArrayList<String> paramTypes = new ArrayList<String>(bench.getSrcTypes());
		ArrayList<String> paramNames = new ArrayList<String>(bench.getParamNames());
		assert paramTypes.size() == paramNames.size();
		for (int i = 0; i < paramTypes.size(); ++i) {
			builder.append(paramTypes.get(i)).append(' ').append(paramNames.get(i));
			if (i < paramTypes.size() - 1) {
				builder.append(", ");
			}
		}
		builder.append(')');
		return builder.toString();
	}

	protected static String genTest(Benchmark bench) {
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(bench.getTestPath()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
}
