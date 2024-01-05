/**
 * 
 */
package com.perelens;

import com.perelens.engine.api.EventSubscriber;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * @author Steve Branda
 *
 */
public class Msgs {
	
	public static String argNotNull() {
		return "Argument must not be null";
	}
	
	public static String validId() {
		return "Id must not be a null value or an empty string";
	}
	
	public static String badState() {
		return "Invalid program state";
	}
	
	public static String wrongType(Class<?> expected, Class<?> passed) {
		return "Wrong class type encountered. Expected = " + expected.getName() + " Passed = " + passed.getName();
	}
	
	public static String wrongType(String id, Class<?> expected, Class<?> passed) {
		return "Wrong class found for id = " + id + ". Expected = " + expected.getClass() + ". Found = " + passed.getName();
	}
	
	public static String noSuchSimObject(String id, Class<?> type) {
		return "Simulation Object of type = " + type.getName() + " and id = " + id + " does not exist.";
	}
	
	public static String duplicateSimObject(String id, Class<? extends EventSubscriber> type) {
		return "Simulation Object of type = " + type.getName() + " and id = " + id + " already exists.";
	}
	
	public static String timeMustAdvance(long completed, long processing) {
		return "Time must advance. Completed=" + completed + ".  Processing=" + processing;
	}

	public static String mustBeStrictlyPositive(Number passedValue) {
		return "Argument must be strictly positive.  Passed Value = " + passedValue;
	}
	
	public static String valueMustBeBetween(Number lower, Number upper, Number value) {
		return "Value must be between " + lower + " and " + upper + "; passed = " + value;
	}
	
	public static String alreadyInitialized() {
		return "Value has already been initialized";
	}
}
