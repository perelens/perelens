/**
 * 
 */
package com.perelens.simulation.mixed;

import com.perelens.engine.api.AbstractEvent;
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
final class SimEvent extends AbstractEvent{
	
	public SimEvent(EventType type, String producerId, long time, long ordinal, Event[] causedBy) {
		super(producerId, type, time, ordinal, causedBy);
	}

	public SimEvent(EventType type, String producerId, long time, long ordinal, Event causedBy) {
		super(producerId, type, time, ordinal, causedBy);
		
	}
	public SimEvent(EventType type, String producerId, long time, long ordinal) {
		super(producerId,type,time, ordinal);
	}
}
