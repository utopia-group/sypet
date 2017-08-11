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
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.utexas.sypet.util.TimeUtil;
import uniol.apt.adt.StructuralExtensionRemover;
import uniol.apt.adt.exception.ArcExistsException;
import uniol.apt.adt.exception.StructureException;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;
import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.exception.UnboundedException;

/**
 * This class represents a k-bounded reachability graph of a Petri net. Let's
 * first define the reachability graph: The reachable markings of a Petri net
 * form a graph. The arcs between markings each belong to a transition which is
 * fired for getting from one transition to another. This graph may be
 * infinitely large.
 *
 * @author Yu Feng
 */
public class ReachabilityGraph {

	// The Petri net that we are handling
	private final PetriNet pn;
	// Map from visited markings to the corresponding nodes
	private final Map<Marking, ReachabilityGraphNode> states = new HashMap<>();
	// List of nodes which were generated but whose enabled transitions weren't
	// handled yet.
	private final Deque<ReachabilityGraphNode> unvisited = new LinkedList<>();
	// List of nodes that were already visited, this is a list to implement
	// iterators.
	private final List<ReachabilityGraphNode> nodes = new ArrayList<>();
	// Are we generating a coverability or a reachability graph?
	private final boolean reachabilityGraph;
	// k-bounded reachability graph.
	private final int K = 3;

	/**
	 * Construct the coverability graph for a given Petri net. If a coverability
	 * graph for this Petri net is already known, that instance is re-used
	 * instead of creating a new one.
	 * 
	 * @param pn
	 *            The Petri net whose coverability graph is wanted.
	 * @return A coverability graph.
	 */
	static public ReachabilityGraph get(PetriNet pn) {
		return get(pn, false);
	}

	/**
	 * Construct the reachability graph for a given Petri net. If a reachability
	 * graph for this Petri net is already known, that instance is re-used
	 * instead of creating a new one. Keep in mind that the reachability graph
	 * of a Petri net can be infinite!
	 * 
	 * @param pn
	 *            The Petri net whose coverability graph is wanted.
	 * @return A coverability graph.
	 */
	static public ReachabilityGraph getReachabilityGraph(PetriNet pn) {
		return get(pn, true);
	}

	/**
	 * Construct the coverability graph for a given Petri net. If a coverability
	 * graph for this Petri net is already known, that instance is re-used
	 * instead of creating a new one.
	 * 
	 * @param pn
	 *            The Petri net whose coverability graph is wanted.
	 * @param reachabilityGraph
	 *            Should just reachability be checked and coverability be
	 *            ignored?
	 * @return A coverability graph.
	 */
	static private ReachabilityGraph get(PetriNet pn, boolean reachabilityGraph) {
		String key = ReachabilityGraph.class.getName();
		if (reachabilityGraph)
			key = key + "-reachability";

		Object extension = null;
		try {
			extension = pn.getExtension(key);
		} catch (StructureException e) {
			// No such extension. Returning "null" would be too easy...
		}

		if (extension != null && extension instanceof ReachabilityGraph)
			return (ReachabilityGraph) extension;

		ReachabilityGraph result = new ReachabilityGraph(pn, reachabilityGraph);
		// Save this coverability graph as an extension, but make sure that it
		// is removed if the structure of
		// the Petri net is changed in any way.
		pn.putExtension(key, result);
		pn.addListener(new StructuralExtensionRemover<PetriNet, Flow, Node>(key));
		return result;
	}

	/**
	 * Construct the coverability graph for a given Petri net. This constructor
	 * is actually cheap. The coverability graph is constructed on-demand when
	 * needed. If you want to force full calculation of the graph, use the
	 * {@link #calculateNodes() calculateNodes} method.
	 * 
	 * @param pn
	 *            The Petri net whose coverability graph is wanted.
	 * @param reachabilityGraph
	 *            Should just reachability be checked and coverability be
	 *            ignored?
	 */
	private ReachabilityGraph(PetriNet pn, boolean reachabilityGraph) {
		this.pn = pn;
		this.reachabilityGraph = reachabilityGraph;
		addNode(null, pn.getInitialMarkingCopy(), null, null);
	}

	/**
	 * Calculate all nodes of the coverability graph.
	 * 
	 * @return Number of nodes in the graph.
	 */
	public int calculateNodes() {
		while (true) {
			if (!visitNode())
				return nodes.size();
		}
	}

	private boolean visitNode() {
		// Pick a random, unvisited node
		// (Here: breadth-first search so that we have short paths to the
		// initial node in checkCover())
		ReachabilityGraphNode node = unvisited.pollFirst();
		if (node == null)
			return false;

		// Make the node generate its postset
		node.getPostsetEdges();
		return true;
	}

	/**
	 * Generate the postset of a given node. This may only be called by
	 * CoverabilityGraphNode.
	 * 
	 * @param node
	 *            Node whose postset should get generated.
	 * @return The node's postset
	 */
	Set<ReachabilityGraphEdge> getPostsetEdges(ReachabilityGraphNode node) {
		// Now follow all activated transitions of that node
		final Marking marking = node.getMarking();
		// bounded construction by K limit.
		if (!bounded(marking))
			return new HashSet<>();

		final Set<ReachabilityGraphEdge> result = new HashSet<>();
		for (Transition t : pn.getTransitions()) {
			if (!t.isFireable(marking)) {
				continue;
			}

			Marking newMarking = t.fire(marking);
			// checkCover() will also change the marking of the Petri net if
			// some OMEGAs are created!
			ReachabilityGraphNode covered = checkCover(newMarking, node);
			result.add(addArc(t, newMarking, node, covered));
		}

		return result;
	}

	/**
	 * Check if the given marking covers any markings on the current path. If
	 * the marking covers some other marking, suitable omegas are inserted.
	 * 
	 * @param cur
	 *            The marking to check.
	 * @param parent
	 *            The immediate parent node.
	 * @return null if no covering occurred, else the node that is covered.
	 */
	private ReachabilityGraphNode checkCover(Marking cur, ReachabilityGraphNode parent) {
		if (reachabilityGraph)
			return null;
		assert parent != null;
		while (parent != null) {
			Marking m = parent.getMarking();
			if (cur.covers(m)) {
				return parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	/**
	 * Add a new arc to the LTS. The arc starts in the marking given at the top
	 * of the current path.
	 * 
	 * @param transition
	 *            The transition for the arc.
	 * @param cur
	 *            The marking to which the arc goes to.
	 * @param from
	 *            The marking from which the arc originates.
	 * @param covered
	 *            node whose marking is covered by the given marking (or null if
	 *            none)
	 */
	private ReachabilityGraphEdge addArc(Transition transition, Marking cur, ReachabilityGraphNode from,
			ReachabilityGraphNode covered) {
		ReachabilityGraphNode state = states.get(cur);
		if (state == null) {
			state = addNode(transition, cur, from, covered);
		}

		return new ReachabilityGraphEdge(transition, from, state);
	}

	/**
	 * Add a node for the given marking to the LTS. Precondition: There is no
	 * node for that marking yet.
	 * 
	 * @param mark
	 *            The marking for which a node should be created.
	 * @param parent
	 *            The parent of this marking. Used for tracing the path to the
	 *            root.
	 * @param covered
	 *            node whose marking is covered by the given marking (or null if
	 *            none)
	 * @return the new node.
	 */
	private ReachabilityGraphNode addNode(Transition transition, Marking mark, ReachabilityGraphNode parent,
			ReachabilityGraphNode covered) {
		assert states.get(mark) == null;

		// Copy the marking to make sure no one else messes with it.
		mark = new Marking(mark);
		ReachabilityGraphNode node = new ReachabilityGraphNode(this, transition, mark, parent, covered);
		states.put(mark, node);
		nodes.add(node);
		// Append it to the tail of the unvisited nodes so that we do a
		// breadth-first search
		unvisited.addLast(node);
		return node;
	}

	/**
	 * Get the initial node of this coverability graph.
	 * 
	 * @return the inital node.
	 */
	public ReachabilityGraphNode getInitialNode() {
		return nodes.get(0);
	}

	/**
	 * Return an iterable for all nodes in this coverability graph.
	 * 
	 * @return an iterable
	 */
	public Iterable<ReachabilityGraphNode> getNodes() {
		return new Iterable<ReachabilityGraphNode>() {

			@Override
			public Iterator<ReachabilityGraphNode> iterator() {
				return new Iterator<ReachabilityGraphNode>() {

					private int position = 0;

					@Override
					public boolean hasNext() {
						do {
							// Are we at the end yet?
							if (position < nodes.size()) {
								return true;
							}

							// Then try generating new nodes and try again
						} while (visitNode());

						return false;
					}

					@Override
					public ReachabilityGraphNode next() {
						// Make sure the next state is generated
						hasNext();
						return nodes.get(position++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Return an iterable for all edges in this coverability graph.
	 * 
	 * @return an iterable
	 */
	public Iterable<ReachabilityGraphEdge> getEdges() {
		final ReachabilityGraph graph = this;
		return new Iterable<ReachabilityGraphEdge>() {

			@Override
			public Iterator<ReachabilityGraphEdge> iterator() {
				return new ReachabilityGraphEdgesIterator(graph);
			}
		};
	}

	/**
	 * Turn this coverability graph into a labeled transition system.
	 * 
	 * @throws UnboundedException
	 *             This exception is thrown when the Petri net is unbounded.
	 * @return The new transition system.
	 * @see #toCoverabilityLTS() For a version of this which does not reject
	 *      unbounded nets.
	 */
	public TransitionSystem toReachabilityLTS() throws UnboundedException {
		return toLTS(true);
	}

	/**
	 * Turn this coverability graph into a labeled transition system.
	 * 
	 * @return The new transition system.
	 * @see #toReachabilityLTS() For a version of this which rejects unbounded
	 *      nets.
	 */
	public TransitionSystem toCoverabilityLTS() {
		try {
			// If this is a reachability graph, we can just use that code (this
			// gets the name of the result
			// right and skips the Omega-check in toLTS(); no UnboundedException
			// can occur since covering is
			// not checked).
			if (reachabilityGraph)
				return toReachabilityLTS();
			return toLTS(false);
		} catch (UnboundedException e) {
			// This should never happen, because we used "false" as the
			// parameter!
			throw new RuntimeException(e);
		}
	}

	/**
	 * Turn this coverability graph into a labeled transition system.
	 * 
	 * @return The new transition system.
	 */
	private TransitionSystem toLTS(boolean onlyReachability) throws UnboundedException {
		String name = (onlyReachability ? "Reachability" : "Coverability") + " graph of " + this.pn.getName();
		Map<Marking, State> ltsStates = new HashMap<>();
		TransitionSystem lts = new TransitionSystem(name);
		lts.putExtension(PetriNet.class.getName(), this.pn);

		for (ReachabilityGraphNode node : this.getNodes()) {
			Marking mark = node.getMarking();
			assert ltsStates.get(mark) == null;

			State n = lts.createState();
			ltsStates.put(mark, n);
			n.putExtension(Marking.class.getName(), mark);
			n.putExtension(ReachabilityGraphNode.class.getName(), node);

			if (onlyReachability && mark.hasOmega()) {
				throw new UnboundedException(this.pn);
			}
		}

		for (ReachabilityGraphNode sourceNode : this.getNodes()) {
			State source = ltsStates.get(sourceNode.getMarking());
			for (ReachabilityGraphEdge edge : sourceNode.getPostsetEdges()) {
				State target = ltsStates.get(edge.getTarget().getMarking());
				Transition transition = edge.getTransition();
				try {
					Arc e = lts.createArc(source.getId(), target.getId(), transition.getLabel());
					e.putExtension(Transition.class.getName(), transition);
					e.putExtension(ReachabilityGraphEdge.class.getName(), edge);
				} catch (ArcExistsException e) {
				}
			}
		}

		// Set up the LTS' initial state
		Marking initialMarking = pn.getInitialMarkingCopy();
		State initialNode = ltsStates.get(initialMarking);
		lts.setInitialState(initialNode);
		assert initialNode != null;

		return lts;
	}

	// the current path
	private Stack<ReachabilityGraphNode> path = new Stack<>();
	// the set of vertices on the path
	private Set<ReachabilityGraphNode> onPath = new HashSet<>();

	private List<Stack<ReachabilityGraphNode>> paths = new ArrayList<>();

	public List<List<Set<Transition>>> allPaths(ReachabilityGraphNode s, ReachabilityGraphNode t) {
		enumerate(s, t);
		List<List<Set<Transition>>> sols = new ArrayList<>();
		for (Stack<ReachabilityGraphNode> stack : paths) {
			List<Set<Transition>> list = new ArrayList<>();
			Stack<ReachabilityGraphNode> revStack = reverse(stack);
			ReachabilityGraphNode src = revStack.pop();
			while (!revStack.isEmpty()) {
				Set<Transition> set = new HashSet<>();
				ReachabilityGraphNode tgt = revStack.pop();
				Set<ReachabilityGraphEdge> edges = src.getPostsetEdgesByTgt(tgt);
				for (ReachabilityGraphEdge e : edges) {
					if (e.getTransition().getId().contains("clone"))
						continue;

					set.add(e.getTransition());
				}

				src = tgt;
				if (set.size() > 0)
					list.add(set);
			}
			sols.add(list);
		}
		return sols;
	}
	
	public boolean isReachable(ReachabilityGraphNode s, ReachabilityGraphNode t) {
		Set<ReachabilityGraphNode> visited = new HashSet<>();
		Stack<ReachabilityGraphNode> worklist = new Stack<>();
		worklist.push(s);
		while (!worklist.isEmpty()) {
			ReachabilityGraphNode worker = worklist.pop();
			if (visited.contains(worker))
				continue;

			if(worker.equals(t)) return true;
			
			visited.add(worker);
			for (ReachabilityGraphNode succ : worker.getPostset()) {
				worklist.push(succ);
			}
		}
		return visited.contains(t);
	}
	
	public List<Set<Transition>> shortestPath(ReachabilityGraphNode s, ReachabilityGraphNode t) {
		Map<ReachabilityGraphNode, Integer> dist = new HashMap<>();
		Map<ReachabilityGraphNode, ReachabilityGraphNode> prev = new HashMap<>();
		List<Set<Transition>> shortest = new ArrayList<>();
		List<ReachabilityGraphNode> path = new ArrayList<>();
		final int INFINITY = 1000;
		Map<ReachabilityGraphNode, Integer> map = new HashMap<>();
		map.put(s, 0);

		// Distance from source to source
		dist.put(s, 0);
		prev.put(t, null);
		LinkedList<ReachabilityGraphNode> worklist = new LinkedList<>();
		int cnt = 0;
		long startNode = System.nanoTime();

		for (ReachabilityGraphNode v : this.getNodes()) {
			if (!v.equals(s)) {
				dist.put(v, INFINITY);
				prev.put(v, null);
			}
			worklist.add(v);
			cnt++;
		}
		System.out.println("reachable nodes: " + cnt);
		long endNode = System.nanoTime();
		TimeUtil.reportTime(startNode, endNode, "Time on all nodes: ");

		long startShort = System.nanoTime();
		while (!worklist.isEmpty()) {
			ReachabilityGraphNode u = s;
			int min = INFINITY;
			for (ReachabilityGraphNode v : worklist) {
				if (dist.get(v) < min) {
					min = dist.get(v);
					u = v;
				}
			}

			boolean flag = worklist.remove(u);
			assert flag : "remove fail.";
			if (u.equals(t))
				break;

			assert map.containsKey(u);
			int depth = map.get(u);

//			System.out.println("depth: " + depth);
			depth++;
			for (ReachabilityGraphNode succ : u.getPostset()) {
				int alt = dist.get(u) + 1;

				map.put(succ, depth);
				if (alt < dist.get(succ)) {
					dist.put(succ, alt);
					prev.put(succ, u);
				}
			}
		}

		// exist a path from source to target?
		if (prev.get(t) != null) {
			path.add(t);
			ReachabilityGraphNode cur = t;
			while (!cur.equals(s)) {
				cur = prev.get(cur);
				path.add(cur);
			}
			assert path.contains(s);
		}

		// collect all transitions.

		if (path.size() < 2)
			return shortest;

		Collections.reverse(path);
		ReachabilityGraphNode init = path.get(0);

		for (int i = 1; i < path.size(); i++) {
			ReachabilityGraphNode st = path.get(i);
			Set<Transition> set = new HashSet<>();
			for (ReachabilityGraphEdge e : init.getPostsetEdgesByTgt(st)) {
				set.add(e.getTransition());
			}
			shortest.add(set);
			init = st;
		}
		
		long endShort = System.nanoTime();
		TimeUtil.reportTime(startShort, endShort, "Time on shortest path: ");

		return shortest;
	}

	// use DFS
	private void enumerate(ReachabilityGraphNode v, ReachabilityGraphNode t) {
		path.push(v);
		onPath.add(v);
		if (v.equals(t)) {
			Stack<ReachabilityGraphNode> temp = new Stack<>();
			temp.addAll(path);
			paths.add(temp);
		} else {
			for (ReachabilityGraphNode w : v.getPostset()) {
				if (!onPath.contains(w))
					enumerate(w, t);
			}
		}

		path.pop();
		onPath.remove(v);
	}

	private Stack<ReachabilityGraphNode> reverse(Stack<ReachabilityGraphNode> st) {
		Stack<ReachabilityGraphNode> newSt = new Stack<>();
		while (!st.isEmpty()) {
			newSt.push(st.pop());
		}
		return newSt;

	}

	private boolean bounded(Marking mk) {
		return (mk.sumToken() <= K);
	}

	// filter duplicated paths.
	public List<Transition> partialOrderReduce(List<List<Set<Transition>>> list) {
		// prefer longer path
		List<Transition> sortedList = new ArrayList<>();
		for (Transition t : pn.getTransitions()) {
			if (!t.getId().contains("clone"))
				sortedList.add(t);
		}
		// filter duplicated.
		Collections.sort(sortedList, new Comparator<Transition>() {
			public int compare(Transition s1, Transition s2) {
				// Write your logic here.
				return s1.getId().compareTo(s2.getId());
			}
		});

		List<Transition> validSol = new ArrayList<>();

		for (List<Set<Transition>> sol : list) {
			// dont prefer short path.
			if (sol.size() < sortedList.size())
				continue;

			for (Set<Transition> set : sol) {
				if (set.size() == 1)
					validSol.add(set.iterator().next());
				else {
					for (Transition tr : set) {
						if (sortedList.contains(tr)) {
							validSol.add(tr);
							sortedList.remove(tr);
							break;
						}
					}
				}
			}

			break;
		}

		System.out.println("sorted List:" + sortedList);
		System.out.println("valid List:" + validSol);
		return validSol;
	}

}