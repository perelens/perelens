/**
 * 
 */
package com.perelens.simulation.risk.events;

import com.perelens.engine.api.EventType;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * Enumeration member names are prefixed with 'RE_' because the name() method should return a unique value.
 * 
 * @author Steve Branda
 *
 */
public enum RiskEvent implements EventType {
	//These are the main event types.
	//Every main event type should have a corresponding end event type.
	RE_CONTACT,               
	RE_THREAT,
	RE_VULNERABILITY,
	RE_LOSS,
	
	//These are supporting event types that can be used to mark a time window in which the main event is active
	//It is important that the END events are always after the main events in the enumeration
	RE_CONTACT_END,                 
	RE_THREAT_END,
	RE_VULNERABILITY_END,
	RE_LOSS_END;
	
	public static final String END = "_END";
}
