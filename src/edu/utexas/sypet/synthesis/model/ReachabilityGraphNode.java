/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2012-2013  Members of the project group APT
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package edu.utexas.sypet.synthesis.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Transition;

/**
 * This class represents a node in a reachability graph. A node is labeled with
 * a marking which identifies it uniquely and has a firing sequence with which
 * it can be reached from the initial marking of the underlying Petri net.
 * Additionally, the postset of the node is available.
 * 
 * @author Yu Feng
 */
public class ReachabilityGraphNode {
	private final ReachabilityGraph graph;
	private final Marking marking;
	private final List<Transition> firingSequence;
	private final ReachabilityGraphNode parent;
	private final ReachabilityGraphNode covered;
	private Set<ReachabilityGraphEdge> postsetEdges;

	/**
	 * Construct a new coverability graph node.
	 * 
	 * @param graph
	 *            The graph that this node belongs to.
	 * @param transition
	 *            The transition that is fired from this node's parent to reach
	 *            this new node.
	 * @param marking
	 *            The marking that identifies this node.
	 * @param parent
	 *            The parent node of this node.
	 * @param covered
	 *            The node which is covered by this node.
	 */
	ReachabilityGraphNode(ReachabilityGraph graph, Transition transition, Marking marking, ReachabilityGraphNode parent,
			ReachabilityGraphNode covered) {
		this.graph = graph;
		this.marking = marking;
		this.parent = parent;
		this.covered = covered;
		List<Transition> sequence = new LinkedList<>();
		if (parent != null) {
			sequence.addAll(parent.firingSequence);
			sequence.add(transition);
		} else {
			assert transition == null;
		}
		this.firingSequence = unmodifiableList(sequence);
	}

	/**
	 * Get the parent of this node on the path back to the root of the depth
	 * first search tree.
	 * 
	 * @return the parent or null
	 */
	ReachabilityGraphNode getParent() {
		return this.parent;
	}

	/**
	 * Get the node in the coverability graph that is covered by this node, if
	 * such a node exists.
	 * 
	 * @return the covered node or null
	 * @see getFiringSequenceFromCoveredNode
	 */
	public ReachabilityGraphNode getCoveredNode() {
		return this.covered;
	}

	/**
	 * Get the marking that this node represents.
	 * 
	 * @return The marking.
	 */
	public Marking getMarking() {
		return new Marking(this.marking);
	}

	/**
	 * Get the firing sequence which reaches the marking represented by this
	 * instance from the initial marking of the Petri net.
	 * 
	 * @return The firing sequence.
	 * @see getFiringSequenceFromCoveredNode
	 */
	public List<Transition> getFiringSequence() {
		return this.firingSequence;
	}

	/**
	 * Get the firing sequence which reaches this node from the covered node, or
	 * null. This function can be used together with getFiringSequence to
	 * describe a way to generate tokens in the Petri net. If the return value
	 * is not null, the sequence returned from this function can be fired in an
	 * infinite loop in the marking described by
	 * <code>getCoveredNode.getMarking()</code>, and will increase the number of
	 * token in the Petri net.
	 * 
	 * @return The firing sequence.
	 * @see getCoveredNode
	 * @see getFiringSequence
	 */
	public List<Transition> getFiringSequenceFromCoveredNode() {
		if (this.covered == null)
			return null;
		int coveredSequenceLength = this.covered.getFiringSequence().size();
		return this.firingSequence.subList(coveredSequenceLength, this.firingSequence.size());
	}

	/**
	 * Get all nodes that are in this node's postset.
	 * 
	 * @return the postset.
	 */
	public Set<ReachabilityGraphNode> getPostset() {
		Set<ReachabilityGraphNode> postset = new HashSet<>();
		for (ReachabilityGraphEdge edge : getPostsetEdges()) {
			postset.add(edge.getTarget());
		}
		return unmodifiableSet(postset);
	}

	/**
	 * Get all edges that begin in this node.
	 * 
	 * @return all edges.
	 */
	public Set<ReachabilityGraphEdge> getPostsetEdges() {
		if (postsetEdges == null)
			postsetEdges = unmodifiableSet(graph.getPostsetEdges(this));
		return postsetEdges;
	}

	public Set<ReachabilityGraphEdge> getPostsetEdgesByTgt(ReachabilityGraphNode tgt) {
		if (postsetEdges == null)
			postsetEdges = unmodifiableSet(graph.getPostsetEdges(this));

		Set<ReachabilityGraphEdge> edges = new HashSet<>();
		for (ReachabilityGraphEdge e : postsetEdges) {
			if (e.getTarget().equals(tgt) && !tgt.equals(e.getSource()))
				edges.add(e);
		}
		return edges;
	}

	public String toString() {
		return getMarking().toString();
	}
}
