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
package edu.utexas.sypet.util;

public class TimeUtil {

	public static double computeTime(long start, long end) {
		double diff = end - start;
		return diff / 1e6;
	}

	public static void reportTime(long start, long end, String info) {
		long diff = end - start; 
		System.out.println(info + diff / 1e6);
	}

}
