/**
 * 
 */
package com.perelens.simulation.failure.events;

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
 * Enumeration member names are prefixed with 'FS_' because the name() method should return a unique value.
 * 
 * @author Steve Branda
 *
 */
public enum FailureSimulationEvent implements EventType{
	
	FS_FAILED,
	FS_FAILING_OVER,
	FS_FAILOVER_FAULT,
	FS_REPAIRED,
	FS_RETURN_TO_SERVICE;
}
