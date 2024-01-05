/**
 * 
 */
package com.perelens.simulation.risk;

import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.api.EventType;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.mixed.EventToWindow;
import com.perelens.simulation.risk.events.RiskEvent;
import com.perelens.simulation.risk.events.RiskUnit;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * This function represents a risk that is realized on receiving a trigger event.
 * The magnitude of the realized risk can be random based on an underlying distribution.
 * 
 * @author Steve Branda
 *
 */
public class RealizedRisk extends EventToWindow implements EventFilter {
	
	public static enum CONFIG_KEYS implements ConfigKey{
		RR_TRIGGER,
		RR_RESULT,
		RR_RESULT_UNIT,
		RR_RESULT_MAGNITUDE;
	}

	private RandomGenerator resultGen = RandomGenerator.NULL_GENERATOR;
	private Distribution resultMagnitude = RiskUtils.ZERO_DISTRIBUTION;
	private RiskUnit resultUnit = null;
	
	public RealizedRisk(String id, EventType trigger, RiskEvent result) {
		super(id,trigger,result,RiskUtils.getEndEvent(result));
		Utils.checkNull(trigger);
	}
	
	protected RiskEvent getResult() {
		return (RiskEvent) this.getWindowStartEvent();
	}
	
	public void setResultMagnitude(Distribution magnitude, RiskUnit unit) {
		Utils.checkNull(magnitude);
		Utils.checkNull(unit);
		
		if (resultUnit != null) {
			throw new IllegalStateException(RskMsgs.alreadyInitialized());
		}
		resultMagnitude = magnitude;
		resultUnit = unit;
	}
	
	@Override
	public void initiate(FunctionInfo info) {
		if (resultUnit != null) {
			resultGen = info.getRandomGenerator();
		}
	}
	
	@Override
	public EventGenerator copy() {
		RealizedRisk toReturn = new RealizedRisk(getId(),getTrigger(),getResult());
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(RealizedRisk toSync) {
		super.syncInternalState(toSync);
		if (resultUnit != null) {
			toSync.setResultMagnitude(resultMagnitude.copy(), resultUnit);
		}
		toSync.resultGen = resultGen.copy();
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		var toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.RR_RESULT, getResult().name());
		toReturn.put(CONFIG_KEYS.RR_TRIGGER, getTrigger().name());
		toReturn.put(CONFIG_KEYS.RR_RESULT_UNIT, resultUnit.name());
		toReturn.put(CONFIG_KEYS.RR_RESULT_MAGNITUDE, resultMagnitude.getSetup());
		return toReturn;
	}

	@Override
	protected RiskSimEvent createEvent(String id, EventType type, long time, long ordinal,Event curEvent) {
		var tr = new RiskSimEvent(id,type,time,ordinal,curEvent);
		if (resultUnit != null && type != getWindowEndEvent() && curEvent.getType() == getTrigger()) {
			double ur = resultGen.nextDouble();
			double mag = resultMagnitude.sample(ur);
			tr.setMagnitude(mag, resultUnit);
		}
		return tr;
	}

	@Override
	protected void windowEnded() {
	}
}
