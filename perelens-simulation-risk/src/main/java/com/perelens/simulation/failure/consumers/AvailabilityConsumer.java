/**
 * 
 */
package com.perelens.simulation.failure.consumers;

import com.perelens.Msgs;
import com.perelens.engine.api.Event;

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
public class AvailabilityConsumer extends AbstractFailureConsumer {

	private long totalUptime = 0;
	private long totalDowntime = 0;
	
	public AvailabilityConsumer(String id) {
		super(id);
	}
	
	@Override
	protected void processChange(Event currentEvent) {
		if (isFailed()) {
			totalUptime += currentEvent.getTime() - getLastTime();
		}else {
			totalDowntime += currentEvent.getTime() - getLastTime();
		}
	}

	@Override
	protected void preInvoke() {
		super.preInvoke();
		//Sanity check
		if (getLastTime() != totalUptime + totalDowntime) {
			throw new IllegalStateException(Msgs.badState());
		}
	}

	@Override
	protected void postInvoke() {
		super.postInvoke();
		//Need to assign time between the last event and the end of the window
		long tail =  getTimeWindow() - getLastTime();
		if (isFailed()) {
			totalDowntime += tail;
		}else {
			totalUptime += tail;
		}
	}

	public long getUpTime() {
		return execute(()->{
			return totalUptime;
		});
	}
	
	public long getDownTime() {
		return execute(()->{
			return totalDowntime;
		});
	}
	
	public double getAvailability() {
		return execute(()-> {
			return totalUptime/(double)(totalUptime + totalDowntime);
		});
	}
}
