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

/**
 * An ordered 3-tuple of objects.
 * 
 * @param    <T0>    The type of the 0th object in the 3-tuple.
 * @param    <T1>    The type of the 1st object in the 3-tuple.
 * @param    <T2>    The type of the 2nd object in the 3-tuple.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Trio<T0, T1, T2> implements java.io.Serializable {
    private static final long serialVersionUID = -4721611129655917301L;
    /**
     * The 0th object in the ordered 3-tuple.
     */
    public T0 val0;
    /**
     * The 1st object in the ordered 3-tuple.
     */
    public T1 val1;
    /**
     * The 2nd object in the ordered 3-tuple.
     */
    public T2 val2;
    public Trio(T0 val0, T1 val1, T2 val2) {
        this.val0 = val0;
        this.val1 = val1;
        this.val2 = val2;
    }
    
    @SuppressWarnings("rawtypes")
	public boolean equals(Object o) {
        if (o instanceof Trio) {
            Trio that = (Trio) o;
            return areEqual(this.val0, that.val0) &&
                   areEqual(this.val1, that.val1) &&
                   areEqual(this.val2, that.val2);
        }
        return false;
    }
    public int hashCode() {
        return (val0 == null ? 0 : val0.hashCode()) +
               (val1 == null ? 0 : val1.hashCode()) +
               (val2 == null ? 0 : val2.hashCode());
    }
    public String toString() {
        return "<" + val0 + ", " + val1 + ", " + val2 + ">";
    }
    
    /**
     * Returns true if the given objects are equal, namely, they are both null or they are equal by the {@code equals()} method.
     *
     * @param x the first compared object.
     * @param y the second compared object.
     *
     * @return true if the given objects are equal.
     */
    public static boolean areEqual(final Object x, final Object y) {
        return x == null ? y == null : x.equals(y);
    }
}