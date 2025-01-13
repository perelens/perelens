/**
 * 
 */
package com.perelens.simulation.core;

import com.perelens.Msgs;

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
class SimMsgs extends Msgs{

	static String unknownDistributionSpec(String setup) {
		return "Unknown distribution specification. Passed =" + setup;
	}
	
	static String argumentIsFinal(String name) {
		return name + " argument is final and may not be set more than once.";
	}

	public static String requestedBeforeReturned(String requestorId) {
		return "Producer requested another resource before recieving and returning the resource from the previous request. Producer id = " + requestorId;
	}

	public static String falseReturn(String producerId) {
		return "Return event recieved from producer that was not granted a resource. Producer id = " + producerId;
	}

	public static String resourceRenewRequired(String requestKey) {
		return "Resource requester failed to renew request at the start of the time window.  Requester id = " + requestKey;
	}

	public static String windowSize() {
		return "Window Size must be greater than 1";
	}

	public static String builderIsDestroyed() {
		return "SimulationBuilder is destroyed";
	}

	public static String badSetupArgument(String setup) {
		return "Invalid setup argument:\n" + setup;
	}

	public static String simulationNotPaused() {
		return "Simulation is not PAUSED.";
	}

	public static String invalidPauseTarget(long pauseAfterTime, long timeExecuted) {
		return "Invalid pauseAfterTime target. Target = " + pauseAfterTime + ". Time Executed = " + timeExecuted;
	}

	public static String simulationNotRunning() {
		return "Simulation is not RUNNING";
	}

	public static String randomGeneratorNotSet() {
		return "RandomGenerator not set";
	}
	
	public static String timeTranslationNotEnabled() {
		return "Time translation is not enabled for this Simulation.";
	}
	
	public static String upperBoundMustBeGreater(Number upper, Number lower) {
		return "Upper bound must be greater than lower bound. uppper = " + upper + "; lower = " + lower;
	}
	
}
