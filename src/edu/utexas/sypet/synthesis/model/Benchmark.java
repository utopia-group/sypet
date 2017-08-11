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

import java.util.List;
import java.util.Set;

public class Benchmark {

	private int id;

	private String methodName;

	private List<String> srcTypes;

	private String tgtType;

	private Set<String> packages;

	private List<String> solutions;

	private List<String> libs;

	private List<String> paramNames;
	
	private String methodHeader;

	private String testPath;

	private String testBody;

	private String body;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public List<String> getSrcTypes() {
		return srcTypes;
	}

	public void setSrcTypes(List<String> srcTypes) {
		this.srcTypes = srcTypes;
	}

	public String getTgtType() {
		return tgtType;
	}

	public void setTgtType(String tgtType) {
		this.tgtType = tgtType;
	}

	public Set<String> getPackages() {
		return packages;
	}

	public void setPackages(Set<String> packages) {
		this.packages = packages;
	}

	public List<String> getSolutions() {
		return solutions;
	}

	public void setSolutions(List<String> solutions) {
		this.solutions = solutions;
	}
	
	public List<String> getLibs() {
		return libs;
	}

	public void setLibs(List<String> libs) {
		this.libs = libs;
	}
	
	public String getMethodHeader() {
		return methodHeader;
	}

	public void setMethodHeader(String methdHeader) {
		this.methodHeader = methdHeader;
	}

	public String getTestPath() {
		return testPath;
	}

	public void setTestPath(String testPath) {
		this.testPath = testPath;
	}

	public String getTestBody() {
		return testBody;
	}

	public void setTestBody(String testBody) {
		this.testBody = testBody;
	}

	public List<String> getParamNames() {
		return paramNames;
	}

	public void setParamNames(List<String> paramNames) {
		this.paramNames = paramNames;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
