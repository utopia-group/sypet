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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JNode {

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<JNode> getSuccessors() {
		return successors;
	}
	
	public Set<JNode> getPreds() {
		return preds;
	}
	
	public void addSuccessor(JNode s) {
		successors.add(s);
	}
	
	public void addPred(JNode s) {
		preds.add(s);
	}

	public void addSuccessor(JNode s, JEdge e) {
		outgoingEdges.put(s, e);
		successors.add(s);
	}
	
	public JEdge getOutgoingEdges(JNode s) {
		return outgoingEdges.get(s);
	}

	public String toString() {
		return name;
	}

	protected int id;

	protected String type;

	protected String name;
	
	protected Map<JNode, JEdge> outgoingEdges = new HashMap<>();

	protected Set<JNode> successors = new LinkedHashSet<>();
	
	protected Set<JNode> preds = new LinkedHashSet<>();


}
