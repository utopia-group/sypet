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
package edu.utexas.sypet.synthesis.sat4j;

import uniol.apt.adt.pn.Place;

/**
 * Each PlaceVar denotes a triple(p, t, n): p is the place(Type), t is timestamp
 * and n is # of tokens.
 * 
 * @author yufeng
 *
 */
public class PlaceVar extends Variable {

	private Place place;
	private int tokenNum;

	public PlaceVar(int id, int time, Place p, int num) {
		super(id, time);
		place = p;
		tokenNum = num;
	}

	public Place getPlace() {
		return place;
	}

	public void setPlace(Place place) {
		this.place = place;
	}

	public int getTokenNum() {
		return tokenNum;
	}

	public void setTokenNum(int tokenNum) {
		this.tokenNum = tokenNum;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(place.getId()).append(" time:").append(time).append(" token:").append(tokenNum)
				.append(" solverId:" + (solverId+1));
		return sb.toString();
	}

}
