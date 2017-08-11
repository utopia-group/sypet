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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SigWrapper {

	private String name;

	private String retType;

	private List<String> params = new ArrayList<>();
	
	private List<Pair<String, String>> args = new ArrayList<>();

	private Set<String> pkgList = new HashSet<>();

	public SigWrapper(String methName, String ret, List<String> paramlist, Set<String> pkgs,
			List<Pair<String, String>> arguments) {
		name = methName;
		retType = ret;
		params = paramlist;
		pkgList = pkgs;
		args = arguments;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRetType() {
		return retType;
	}

	public void setRetType(String retType) {
		this.retType = retType;
	}

	public List<String> getParams() {
		return params;
	}

	public void setParams(List<String> params) {
		this.params = params;
	}

	public Set<String> getPkgList() {
		return pkgList;
	}

	public void setPkgList(Set<String> pkgList) {
		this.pkgList = pkgList;
	}
	
	public List<Pair<String, String>> getArgs() {
		return args;
	}

	public void setArgs(List<Pair<String, String>> args) {
		this.args = args;
	}
	
	public String toString() {
		return "name: " + name + "\n" + "retType: " + retType + "\n" + "params: " + params + "\n" + "pkgList: "
				+ pkgList;
	}
}
