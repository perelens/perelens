/**
 * 
 */
package com.perelens.simulation.core;

import java.util.Collection;

import com.perelens.engine.api.AbstractEvent;
import com.perelens.engine.api.EventMagnitude;
import com.perelens.engine.api.EventType;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.events.ResourcePoolEvent;

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
public final class ResPoolEvent extends AbstractEvent {

	private long timeOpt = -1;
	
	public ResPoolEvent(String producerId, ResourcePoolEvent type, long time, long ordinal) {
		super(producerId, type, time, ordinal);
	}
	
	public ResPoolEvent(String producerId, ResourcePoolEvent type, long time, long ordinal, Collection<EventType> responseTypes) {
		this(producerId,type,time, ordinal);
		Utils.checkNull(responseTypes);
		setResponseTypes(responseTypes);
	}

	@Override
	public EventMagnitude getMagnitude() {
		return null;
	}

	public long getTimeOptimization() {
		return timeOpt;
	}

	public void setTimeOptimization(long timeOptimization) {
		Utils.checkArgNotNegative(timeOptimization);
		if (timeOpt != -1) {
			throw new IllegalStateException(SimMsgs.alreadyInitialized());
		}
		this.timeOpt = timeOptimization;
	}

}
