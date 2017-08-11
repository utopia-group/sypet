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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class is used for iterating over the edges of a k-bounded reachability graph.
 * @author Yu Feng
 */
class ReachabilityGraphEdgesIterator implements Iterator<ReachabilityGraphEdge> {
	private final Iterator<ReachabilityGraphNode> nodeIter;
	private Iterator<ReachabilityGraphEdge> edgeIter;

	/**
	 * Constructor
	 * @param cover {@link ReachabilityGraph} to iterate over
	 */
	ReachabilityGraphEdgesIterator(ReachabilityGraph cover) {
		nodeIter = cover.getNodes().iterator();
		if (nodeIter.hasNext())
			edgeIter = nodeIter.next().getPostsetEdges().iterator();
	}

	/**
	 * This function updates the edgeIter member. It will either have a valid next element after this function or it
	 * will be null if all edges were iterated over.
	 * @return True if the nodeIter has a next element.
	 */
	private boolean updateEdgeIter() {
		while (!edgeIter.hasNext()) {
			if (nodeIter.hasNext()) {
				edgeIter = nodeIter.next().getPostsetEdges().iterator();
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean hasNext() {
		return updateEdgeIter();
	}

	@Override
	public ReachabilityGraphEdge next() {
		if (updateEdgeIter())
			return edgeIter.next();
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}


