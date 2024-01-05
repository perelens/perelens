/**
 * 
 */
package com.perelens.engine.utils;

import java.util.Arrays;

import com.perelens.engine.api.Event;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * @author Steve Branda
 *
 */
public class Utils {
	
	public static final long[] EMPTY_LONG_ARRAY = new long[0];

	//Generic Logic for appending queues
	public final static <R> R[] append(R[] curList, R toAdd, int insertAt) {
		R[] toReturn = curList;
		if (insertAt == curList.length) {
			if (curList.length == 0) {
				toReturn = Arrays.copyOf(curList, 4);
			}else{
				toReturn = Arrays.copyOf(curList, curList.length + curList.length);
			}
		}
		toReturn[insertAt] = toAdd;
		return toReturn;
	}
	
	public final static long[] append(long[] curList, long toAdd, int insertAt) {
		long[] toReturn = curList;
		if (insertAt == curList.length) {
			if (curList.length == 0) {
				toReturn = Arrays.copyOf(curList, 4);
			}else{
				toReturn = Arrays.copyOf(curList, curList.length + curList.length);
			}
		}
		toReturn[insertAt] = toAdd;
		return toReturn;
	}

	public final static <R> int compress(R[] toCompress, int nextIndex) {
		int offset = 0;
		for (int i = 0; i < nextIndex; i++) {
			if (toCompress[i] == null) {
				offset++;
			}else if (offset > 0) {
				//Shift the entry back
				toCompress[i - offset] = toCompress[i];
				toCompress[i] = null;
			}
		}
		
		return nextIndex - offset;
	}

	public static final Event[] EMPTY_QUEUE = new Event[0];

	public static void checkId(String id) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("Invalid id: " + id);
		}
	}

	public static void checkNull(Object toCheck) {
		if (toCheck == null) {
			throw new IllegalArgumentException("Argument must not be null");
		}
	}

	public final static void checkPercentage(double percentage) {
		if (percentage < 0d || percentage > 1d) {
			throw new IllegalArgumentException(invalidPercentage(percentage));
		}
	}
	
	private static String invalidPercentage(double percentage) {
		return "Invalid Percentage.  Value must be in range [0,1].  Passed = " + percentage;
	}

	public final static void checkArgStrictlyPositive(long arg) {
		if (arg < 1) {
			throw new IllegalArgumentException();
		}
	}
	
	public final static void checkArgStrictlyPositive(double arg) {
		if (arg <= 0.0) {
			throw new IllegalArgumentException();
		}
	}
	
	public final static void checkArgNotNegative(long arg) {
		if (arg < 0) {
			throw new IllegalArgumentException();
		}
	}
	
	public final static void checkIsFinite (double val) {
		if (!Double.isFinite(val)) {
			throw new IllegalArgumentException();
		}
	}
	
	public final static boolean processIsSuccessful(double successPercentage, double uniformRandom) {
		Utils.checkPercentage(successPercentage);
		if (uniformRandom < successPercentage) {
			return true;
		}else {
			return false;
		}
	}
	
	public final static boolean processFails(double failPercentage, double uniformRandom) {
		Utils.checkPercentage(failPercentage);
		if (uniformRandom < failPercentage) {
			return true;
		}else {
			return false;
		}
	}
	
	public final static void checkUniformRandom(double uniformRandom) {
		if (uniformRandom < 0d || uniformRandom >= 1.0d) {
			throw new IllegalArgumentException();
		}
	}
}
