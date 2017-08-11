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

import uniol.apt.adt.pn.Transition;

/**
 * This class represents an edge in a k-bounded reachability graph. An edge has
 * a source and a target node and is labeled with a transition.
 * 
 * @author Yu Feng
 */
public class ReachabilityGraphEdge {
	private final Transition transition;
	private final ReachabilityGraphNode source;
	private final ReachabilityGraphNode target;

	/**
	 * Construct a new coverability graph edge for the given arguments.
	 * 
	 * @param transition
	 *            The transition that the edge is labeled with.
	 * @param source
	 *            The node that the edge begins in.
	 * @param target
	 *            The node that the edge goes to.
	 */
	ReachabilityGraphEdge(Transition transition, ReachabilityGraphNode source, ReachabilityGraphNode target) {
		this.transition = transition;
		this.source = source;
		this.target = target;
	}

	/**
	 * Get the transition that this edge was fired for. * @return the
	 * transition.
	 */
	public Transition getTransition() {
		return this.transition;
	}

	/**
	 * Get the node that this edge leads to.
	 * 
	 * @return the target.
	 */
	public ReachabilityGraphNode getTarget() {
		return this.target;
	}

	/**
	 * Get the node that this edge originates from.
	 * 
	 * @return the source.
	 */
	public ReachabilityGraphNode getSource() {
		return this.source;
	}

	public String toString() {
		return source.getMarking() + "[" + transition + "]" + target.getMarking();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
