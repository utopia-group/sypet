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

import edu.utexas.sypet.util.SynthUtil;

/**
 * An ordered 2-tuple of objects.
 * 
 * @param	<T0>	The type of the 0th object in the ordered 2-tuple.
 * @param	<T1>	The type of the 1st object in the ordered 2-tuple.
 * 
 * @author Yu Feng (yufeng@cs.utexas.edu)
 */
public class Pair<T0, T1> implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8922893589568667796L;
	/**
	 * The 0th object in the ordered 2-tuple.
	 */
	public T0 val0;
	/**
	 * The 1st object in the ordered 2-tuple.
	 */
	public T1 val1;
	public Pair(T0 val0, T1 val1) {
		this.val0 = val0;
		this.val1 = val1;
	}
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			@SuppressWarnings("rawtypes")
			Pair that = (Pair) o;
			return SynthUtil.equal(this.val0, that.val0) &&
					SynthUtil.equal(this.val1, that.val1);
		}
		return false;
	}
	public int hashCode() {
		return (val0 == null ? 0 : val0.hashCode()) +
			   (val1 == null ? 0 : val1.hashCode());
	}
	public String toString() {
		return "<" + val0 + ", " + val1 + ">";
	}
}