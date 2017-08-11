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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class JGraph {

	protected Map<Integer, JNode> id2Node = new HashMap<>();

	protected Map<JNode, Integer> node2Id = new HashMap<>();

	protected Map<String, JNode> name2Node = new HashMap<>();

	protected boolean directed;

	protected String type;

	protected String label;

	protected Set<JNode> nodes = new LinkedHashSet<>();

	protected Set<JEdge> edges = new LinkedHashSet<>();
	
	protected int K = 7;
	
	protected int nodeCounter = 1;

	protected int edgeCounter = 10000;
	
	protected Map<Pair<String, String>, JEdge> edgeMap = new HashMap<>();

	public boolean isDirected() {
		return directed;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Set<JNode> getNodes() {
		return nodes;
	}

	public boolean containNode(JNode n) {
		return nodes.contains(n);
	}

	public void setNodes(Set<JNode> nodes) {
		this.nodes = nodes;
	}

	public Set<JEdge> getEdges() {
		return edges;
	}

	public void setEdges(Set<JEdge> edges) {
		this.edges = edges;
	}
	
	public void addEdge(String src, String tgt) {
		JNode srcNode = null;
		JNode tgtNode = null;
		if (name2Node.containsKey(src)) {
			srcNode = name2Node.get(src);
		} else {
			srcNode = new JNode();
			srcNode.setName(src);
			name2Node.put(src, srcNode);
			int index = nodeCounter++;
			id2Node.put(index, srcNode);
			node2Id.put(srcNode, index);
			nodes.add(srcNode);
		}

		if (name2Node.containsKey(tgt)) {
			tgtNode = name2Node.get(tgt);
		} else {
			tgtNode = new JNode();
			tgtNode.setName(tgt);
			name2Node.put(tgt, tgtNode);
			int index = nodeCounter++;
			id2Node.put(index, tgtNode);
			node2Id.put(tgtNode, index);
			nodes.add(tgtNode);
		}
		assert tgtNode != null;
		assert srcNode != null;
		srcNode.addSuccessor(tgtNode);
		tgtNode.addPred(srcNode);
		Pair<String, String> pair = new Pair<>(src, tgt);
		if (!edgeMap.containsKey(pair)) {
			JEdge e = new JEdge(node2Id.get(srcNode), node2Id.get(tgtNode));
			edges.add(e);
			srcNode.addSuccessor(tgtNode, e);
		}
	}
	
	public Set<String> backwardReach(String src) {
		Map<JNode, Integer> map = new HashMap<>();
		JNode n = name2Node.get(src);
		assert n != null;
		map.put(n, 0);
		Set<String> set = new LinkedHashSet<>();
		LinkedList<JNode> worklist = new LinkedList<>();
		worklist.push(n);
		Set<JNode> visited = new LinkedHashSet<>();
		while (!worklist.isEmpty()) {
			JNode worker = worklist.pollLast();
			if (visited.contains(worker)) {
				continue;
			}

			visited.add(worker);
			int cnt = map.get(worker);
			if (cnt > K) {
				continue;
			}
			cnt++;

			set.add(worker.getName());
			for (JNode pred : worker.getPreds()) {
				if (pred.equals(worker))
					continue;
				map.put(pred, cnt);
				worklist.push(pred);
			}
		}
		return set;
	}
	
	public Set<String> backwardReach2(String src) {
		JNode tgtNode = getNode(src);
		Set<String> set = new LinkedHashSet<>();
		for(JNode node : nodes) {
			if(!node.getName().equals(src)) {
				List<JNode> dist = this.Dijkstra(node, tgtNode);
				if(dist.isEmpty()) continue;
				
				if((dist.size() - 1) <= K) {
					set.add(node.getName());
				}
			} else {
				set.add(src);
			}
		}
		return set;
	}

	public boolean isReachable(JNode source, JNode target) {
		Set<JNode> visited = new HashSet<>();
		Stack<JNode> worklist = new Stack<>();
		worklist.push(source);
		while (!worklist.isEmpty()) {
			JNode worker = worklist.pop();
			if (visited.contains(worker))
				continue;

			visited.add(worker);
			for (JNode succ : worker.getSuccessors()) {
				worklist.push(succ);
			}
		}
		return visited.contains(target);
	}

	public List<JNode> Dijkstra(JNode source, JNode target) {
		Map<JNode, Integer> dist = new HashMap<>();
		Map<JNode, JNode> prev = new HashMap<>();
		List<JNode> path = new ArrayList<>();
		final int INFINITY = 1000;

		if (!isReachable(source, target))
			return path;

		// Distance from source to source
		dist.put(source, 0);
		prev.put(source, null);
		LinkedList<JNode> worklist = new LinkedList<>();
		for (JNode v : nodes) {
			if (!v.equals(source)) {
				dist.put(v, INFINITY);
				prev.put(v, null);
			}
			worklist.add(v);
		}

		while (!worklist.isEmpty()) {
			JNode u = source;
			int min = INFINITY;
			for (JNode v : worklist) {
				if (dist.get(v) < min) {
					min = dist.get(v);
					u = v;
				}
			}
			boolean flag = worklist.remove(u);
			assert flag : "remove fail.";
			if (u.equals(target))
				break;

			for (JNode succ : u.getSuccessors()) {
				JEdge e = u.getOutgoingEdges(succ);
				int alt = dist.get(u) + 1;
				
				if ((e.getLabel() != null) && (!e.getLabel().equals("?")))
					alt = dist.get(u) + 10;
				
				if (alt < dist.get(succ)) {
					dist.put(succ, alt);
					prev.put(succ, u);
				}
			}
		}

		// exist a path from source to target?
		if (prev.get(target) != null) {
			path.add(target);
			JNode cur = target;
			while (!cur.equals(source)) {
				cur = prev.get(cur);
				path.add(cur);
			}
			assert path.contains(source);
		}

		return path;
	}

	public void buildDependency() {
		for (JNode n : nodes) {
			id2Node.put(n.getId(), n);
			name2Node.put(n.getName(), n);
		}

		for (JEdge e : edges) {
			JNode src = id2Node.get(e.getSource());
			JNode tgt = id2Node.get(e.getTarget());
			src.addSuccessor(tgt, e);
		}
	}

	public JNode getNode(int i) {
		assert id2Node.containsKey(i);
		return id2Node.get(i);
	}

	public JNode getNode(String s) {
		assert name2Node.containsKey(s) : s;
		return name2Node.get(s);
	}
	
	public void setK(int val) {
		K = val;
	}
	
	public Set<JNode> upperCastSet(JNode src) {
		LinkedList<JNode> worklist = new LinkedList<>();
		worklist.add(src);
		LinkedHashSet<JNode> visited = new LinkedHashSet<>();
		while (!worklist.isEmpty()) {
			JNode worker = worklist.poll();
			if (visited.contains(worker))
				continue;

			visited.add(worker);
			for (JNode succ : worker.getSuccessors()) {
				JEdge e = worker.getOutgoingEdges(succ);
				if (e.getLabel().equals("?"))
					worklist.add(succ);
			}
		}
		
		//remove itself.
		visited.remove(src);
		return visited;
	}


	public String toString() {
		return label + " | nodes: " + nodes + " edges: " + edges;
	}
}
