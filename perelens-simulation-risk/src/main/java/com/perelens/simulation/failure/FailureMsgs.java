/**
 * 
 */
package com.perelens.simulation.failure;

import com.perelens.Msgs;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventType;

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
class FailureMsgs extends Msgs {

	static String usageKofN() {
		return "K and N arguments must satisfy 0 < K < N";
	}

	static String missingDependencies(int size, int n) {
		return "Expected at least " + n + " dependencies, but only " + size + " reported";
	}

	public static String badRestoreTime(int systemRestore) {
		return "System Restore Time must be a positive integer. Passed = " + systemRestore;
	}

	public static String badUniformRandom(double uniformRandom) {
		return "Invalid uniform random.  Value must be in range [0,1).  Passed = " + uniformRandom;
	}

	public static String reconfigurationNotAllowed() {
		return "Simulation functions may not be reconfigured once the simulation has started";
	}

	public static String mustBeGreaterThanOrEqualToZero(int mtfo) {
		return "Argument must be greater than or equal to zero. Passed = " + mtfo;
	}

	public static String invalidActiveNodes(int activeNodes, int k, int n) {
		return "Active Nodes must be >= k or <= n. Active Nodes = " + activeNodes + ". k = " + k + ". n = " + n;
	}

	public static String badTotalFailureState(int totalFailures, int idx) {
		return "Expected = " + totalFailures + ". Found = " + idx;
	}

	public static String unableToClearFailureEvent(Event repairEvent) {
		return "Could not find original failure event for repair event:\n" + repairEvent;
	}

	public static String wrongEventType(EventType expected, EventType passed) {
		return "Wrong EventType. Expected = " + expected + ". Passed = " + passed;
	}

	public static String extraDependencies(int size, int n) {
		return "Extra dependencies detected: n = " + n + ", but " + size + "dependencies reported";
	}

}
