package edu.cmu.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import com.google.gson.Gson;

import edu.utexas.sypet.synthesis.PathFinder;
import edu.utexas.sypet.synthesis.Sketcher;
import edu.utexas.sypet.synthesis.SypetTestUtil;
import edu.utexas.sypet.synthesis.model.Benchmark;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.sat4j.PetrinetEncoding.Option;
import edu.utexas.sypet.util.SootUtil;
import edu.utexas.sypet.util.TimeUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.util.ServerRunner;
import soot.CompilationDeathException;
import soot.Scene;
import soot.options.Options;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;

/**
 * A custom subclass of NanoHTTPD.
 */
public class HttpServer extends NanoHTTPD {

	private static final Logger LOG = Logger.getLogger(HttpServer.class.getName());
	private static int httpServerPort = 9092;

	public static boolean VERBOSE = false;
	public static String benchLoc = null;
	public static long TIMEOUT = 600000;

	public static final int maxTokens = 10;

	public static Option objectiveOption = Option.AT_LEAST_ONE;
	public static int maxIterations = 5;

	public static List<String> clones = new ArrayList<>();
	public static List<String> libsCP = new ArrayList<>();

	public static void main(String[] args) {
		// Start the HTTP Server
		ServerRunner.run(HttpServer.class);
	}

	public HttpServer() {
		super(httpServerPort);
		System.out.println("SyPet HttpServer starting on port: " + httpServerPort);
	}

	public static PathFinder initPetriNet(Benchmark qb, List<PetriNet> pNetList, int pn, int local) {

		if (pNetList.size() == 1) {
			pn = 0;
		}

		PetriNet pNet = pNetList.get(pn);
		HttpServer.LOG.info("PetriNet for path length: " + (local-1) + " [places: " + pNet.getPlaces().size()
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

	public String runSyPet(String bench) {

		clones.clear();
		long startSoot = System.nanoTime();

		// options
		int roundRobinPosition = 0;
		int roundRobinIterations = 0;
		int roundRobinIterationsLimit = 40;
		int roundRobinRange = 3;
		boolean roundRobinFlag = false;
		objectiveOption = Option.AT_LEAST_ONE;
		maxIterations = 5;

		// statistics to run SyPet
		double timeGetPath = 0;
		double timeInitSketch = 0;
		double timeFillHoles = 0;
		double timeCompilation = 0;
		double timeRunTest = 0;
		double timeTotal = 0;
		long cntFillHoles = 0;
		try {
			Benchmark qb = new Gson().fromJson(bench, Benchmark.class);
			// generate the method header
			qb.setMethodHeader(genMethodHeader(qb));

			// TODO: map from packages to required libraries
			ArrayList<String> libs = new ArrayList<>();
			libs.add("./lib/rt7.jar");
			//libs.add("../lib/simplepoint.jar");
			qb.setLibs(libs);

			/////////////////////////////////////////////////////////////////////////////////
			HttpServer.LOG.info("--------------------------------------------------------");
			HttpServer.LOG.info("Benchmark Id: " + qb.getId());
			HttpServer.LOG.info("Method name: " + qb.getMethodName());
			HttpServer.LOG.info("Packages: " + qb.getPackages());
			HttpServer.LOG.info("Libraries: " + qb.getLibs());
			HttpServer.LOG.info("Source type(s): " + qb.getSrcTypes());
			HttpServer.LOG.info("Target type: " + qb.getTgtType());
			HttpServer.LOG.info("--------------------------------------------------------");
			///////////////////////////////////////////////////
			Set<String> pkgs = qb.getPackages();

			StringBuilder options = new StringBuilder();
			options.append("-prepend-classpath");
			options.append(" -full-resolver");
			options.append(" -allow-phantom-refs");
			StringBuilder cp = new StringBuilder();
			for (String lib : qb.getLibs()) {
				if (!libsCP.contains(lib)) {
					cp.append(lib);
					cp.append(":");
					options.append(" -process-dir " + lib);
					libsCP.add(lib);
				}
			}
			if (!cp.toString().isEmpty())
				options.append(" -cp " + cp.toString());
			if (!Options.v().parse(options.toString().split(" ")))
				throw new CompilationDeathException(CompilationDeathException.COMPILATION_ABORTED,
						"Error: Parsing libraries.");

			Scene.v().loadBasicClasses();
			Scene.v().loadNecessaryClasses();

			List<PetriNet> pNetList = new ArrayList<>();

			// only one petrinet without pruning.
			PetriNet pNet = new PetriNet();
			for (String lib : qb.getLibs()) {
				SootUtil.processJar(lib, pkgs, pNet);
			}
			SootUtil.handlePolymorphism(pNet);
			HttpServer.LOG.info("#Classes: " + SootUtil.classNum);
			HttpServer.LOG.info("#Methods: " + SootUtil.methodNum);
			long endSoot = System.nanoTime();
			double sootTime = TimeUtil.computeTime(startSoot, endSoot);
			HttpServer.LOG.info("Soot Time: " + sootTime);
			pNetList.add(pNet);

			// Multiple args: check if we need put clone as initial constraints.
			clones = SootUtil.getClones(qb.getSrcTypes());

			int petriIterator = 0;

			int cnt = 0;
			int localMax = 2;
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
				} else {
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
					// init sketcher.
					long start2 = System.nanoTime();
					Sketcher sk = new Sketcher(solution, pf.getVars(), pf.getTgt());
					sk.initSketch();
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
						timeTotal = TimeUtil.computeTime(start0, end0);
						if (passTest) {
							HttpServer.LOG.info("=========Statistics (time in milliseconds)=========");
							HttpServer.LOG.info("Benchmark Id: " + qb.getId());
							HttpServer.LOG.info("Sketch Generation Time: " + timeGetPath);
							HttpServer.LOG.info("Sketch Completion Time: " + (timeInitSketch + timeFillHoles));
							HttpServer.LOG.info("Compilation Time: " + timeCompilation);
							HttpServer.LOG.info("Running Test cases Time: " + timeRunTest);
							HttpServer.LOG.info(
									"Synthesis Time: " + (timeGetPath + timeInitSketch + timeFillHoles + timeRunTest));
							HttpServer.LOG.info("Total Time: "
									+ (timeGetPath + timeInitSketch + timeFillHoles + timeRunTest + timeCompilation));
							HttpServer.LOG.info("Number of components: " + res.size());
							HttpServer.LOG.info("Number of holes: " + sk.getHolesNum());
							HttpServer.LOG.info("Number of completed programs: " + cntFillHoles);
							HttpServer.LOG.info("Number of sketches: " + cnt);
							HttpServer.LOG.info("Solution:\n " + snippet.replace(";", ";\n "));
							HttpServer.LOG.info("============================");
							return snippet.replace(";", ";\n ");
						} else if (timeTotal > TIMEOUT) {
							HttpServer.LOG.info("=========Statistics=========");
							HttpServer.LOG.info("Benchmark Id: " + qb.getId());
							HttpServer.LOG.info("Sketch Generation Time: " + timeGetPath);
							HttpServer.LOG.info("Sketch Completion Time: " + (timeInitSketch + timeFillHoles));
							HttpServer.LOG.info("Compilation Time: " + timeCompilation);
							HttpServer.LOG.info("Running Test cases Time: " + timeRunTest);
							HttpServer.LOG.info("Number of completed programs: " + cntFillHoles);
							HttpServer.LOG.info("Number of sketches: " + cnt);
							HttpServer.LOG.info("TIMEOUT after " + TIMEOUT + " ms");
							HttpServer.LOG.info("============================");
							return "// SyPet timeout! We are sorry but we could not find a program in less than 5 minutes!";
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
				return "// SyPet failure!";
		}
		return "// SyPet failure!";

	}

	@Override
	public Response serve(IHTTPSession session) {

		String response = "";
		try {
			Method method = session.getMethod();
			String uri = session.getUri();
			HttpServer.LOG.info(method + " '" + uri + "' ");
			session.getHeaders();

			if (session.getMethod() == Method.POST) {

				Map<String, String> form = new HashMap<String, String>();
				session.parseBody(form);
				String benchmark = "";
				for (String s : form.keySet()) {
					benchmark += form.get(s);
				}
				response = runSyPet(benchmark);
				Response resp = newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, response);
				resp.addHeader("Access-Control-Allow-Origin", "*");
				resp.addHeader("Access-Control-Allow-Methods", "POST");
				return resp;

			}

		} catch (Exception e) {
			System.out.println("e = " + e);
			response = "// SyPet error: Http Server exception!";
			Response resp = newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, response);
			resp.addHeader("Access-Control-Allow-Origin", "*");
			resp.addHeader("Access-Control-Allow-Methods", "POST");
			return resp;
		}

		response += "// SyPet error: Http Server does not support the received request!";
		Response resp = newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, response);
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "POST");
		return resp;
	}
}
