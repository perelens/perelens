/**
 * 
 */
package com.perelens.engine.core;

import java.util.Collection;

import com.perelens.Msgs;
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
class EngineMsgs extends Msgs{

	static String globalConsumer() {
		return "EventConsumer is registered as a global consumer and may not subscribe to individual event streams.";
	}
	
	static String nonGlobalConsumer() {
		return "EventConsumer is registered as regular consumer and may not subscribe to the global event stream.";
	}
	
	static String mayNotUnregister(Class<?> type) {
		return "Instances of " + type.getName() + " may not be unregistered";
	}
	
	static String globalConsumerType() {
		return "Global consumers may not implement EventEvaluator";
	}
	
	static String selfSubscription(String id) {
		return "EventEvaluators may not subscribe to their own events. Passed Id = " + id;
	}
	
	static String unusedSimulationObject(String id) {
		return "Simulation Object has no dependencies or subscribers. id = " + id;
	}
	
	static String engineNotReentrant() {
		return "CoreEngine methods should not be called in an reentrant manner.";
	}

	static String engineNotConcurrent() {
		return "CoreEngine methods should not be invoked concurrently by multiple threads";
	}
	
	static String badOffset(long current, long requested) {
		return "Requested offset must be greater than the current offset.  Current = " + current + ".  Requested = " + requested;
	}

	static String outsideTimeWindow(long time, long timeOffset, long targetOffset) {
		return "Event time must be greater than " + timeOffset + " and less than or equal to " + targetOffset + ". Event Time = " + time;
	}

	static String noEventSpoofing(String id, String producerId) {
		return "EventGenerator with id = " + id + " attempted to raise an event with an invalid producer id = " + producerId; 
	}

	public static String engineStateCorrupt() {
		return "Engine state corrupted.  Instance can only be destroyed";
	}

	public static String badResponseType(Collection<EventType> responseTypes, EventType type) {
		return "Bad response type.  Expected = " + responseTypes + " . Passed = " + type;
	}

	public static String circularDependencyDetected() {
		return "Circular dependency detected";
	}
}
