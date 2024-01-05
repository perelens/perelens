/**
 * 
 */
package com.perelens.simulation.risk;

import com.perelens.Msgs;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.risk.events.RiskEvent;

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
class RskMsgs extends Msgs {

	static String invalidEndEventUse(RiskEvent event) {
		return "END event types not valid for this operation.  EventType passed = " + event;
	}

	public static String triggerMayNotBeCondition(RiskEvent condition) {
		return "Event type cannot be both a Trigger and a Condition.  Event Type passed = " + condition;
	}

	public static String newWindowStartBeforePreviousEnds() {
		return "Window start event recieved while the previous event window is still active";
	}

	public static String startAndEndEventNotSame(EventType winStart) {
		return "Window start and end event types must be different.  Passed = " + winStart;
	}

}
