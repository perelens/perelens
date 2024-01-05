/**
 * 
 */
package com.perelens.simulation.failure.consumers;

import com.perelens.engine.api.AbstractEventConsumer;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.simulation.failure.events.FailureSimulationEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Event consumer that listens for FP_FAILED and FP_RETURN_TO_SERVICE events from a specific function in order to collect availability metrics.
 * The implementation is thread safe but is not intended to be called under contention.
 * Calls to the data retrieving methods should not be called while the Simulation Engine is simulating a time window.
 * They should be called after the simulation is complete or between windows invocations.
 *
 * 
 * @author Steve Branda
 *
 */
public abstract class AbstractFailureConsumer extends AbstractEventConsumer {

	private boolean failed = false;
	
	protected AbstractFailureConsumer(String id) {
		super(id);
	}
	
	private static final EventFilter EVENT_FILTER = new EventFilter() {

		@Override
		public boolean filter(Event event) {
			return event.getType() == FailureSimulationEvent.FS_FAILED || event.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE;
		}
		
	};
	
	@Override
	public EventFilter getEventFilter() {
		return EVENT_FILTER;
	}

	public boolean isFailed() {
		return failed;
	}
	
	protected abstract void processChange(Event currentEvent);
	
	@Override
	protected void processEvent(Event curEvent) {
		
		if (curEvent.getTime() < getLastTime()){
			throw new IllegalArgumentException("Time must advance");
		}
		if (curEvent.getTime() > getTimeWindow()) {
			throw new IllegalArgumentException("Event should be in future time window");
		}
		
		if (!isFailed()) {
			if (curEvent.getType() == FailureSimulationEvent.FS_FAILED) {
				failed = true;
				processChange(curEvent);
			}else if (curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				throw new IllegalStateException("Must fail before it can be repaired");
			}else {
				throw new IllegalStateException("Bad State");
			}
		}else {
			if (curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				failed=false;
				processChange(curEvent);
			}else if (curEvent.getType() == FailureSimulationEvent.FS_FAILED) {
				throw new IllegalStateException("Must be repaired before it can fail again");
			}else {
				throw new IllegalStateException("Bad State");
			}	
		}
		
	}
}
