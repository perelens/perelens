/**
 * 
 */
package com.perelens.simulation.risk;

import com.perelens.engine.api.AbstractEvent;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventMagnitude;
import com.perelens.engine.api.EventType;
import com.perelens.engine.utils.Utils;

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
final class RiskSimEvent extends AbstractEvent implements EventMagnitude {

	private double magnitude;
	private Unit unit = null;
	
	protected RiskSimEvent(String producerId, EventType type, long time, long ordinal, Event causedBy) {
		super(producerId, type, time, ordinal, causedBy);
	}

	protected RiskSimEvent(String producerId, EventType type, long time, long ordinal, Event[] causedBy) {
		super(producerId, type, time, ordinal, causedBy);
	}
	
	protected RiskSimEvent(String producerId, EventType type, long time, long ordinal, Event[] causedBy, int causeIndex) {
		super(producerId, type, time, ordinal, causedBy, causeIndex);
	}

	protected RiskSimEvent(String producerId, EventType type, long time, long ordinal) {
		super(producerId, type, time, ordinal);
	}
	
	protected void setMagnitude(double magnitude, Unit unit) {
		Utils.checkIsFinite(magnitude);
		Utils.checkNull(unit);
		
		if (this.unit != null) {
			throw new IllegalStateException(RskMsgs.alreadyInitialized());
		}
		
		this.magnitude = magnitude;
		this.unit = unit;
	}

	@Override
	public EventMagnitude getMagnitude() {
		if (unit == null) {
			return EventMagnitude.NO_MAGNITUDE;
		}else {
			return this;
		}
	}

	@Override
	public double magnitude() {
		return magnitude;
	}

	@Override
	public Unit unit() {
		return unit;
	}
}
