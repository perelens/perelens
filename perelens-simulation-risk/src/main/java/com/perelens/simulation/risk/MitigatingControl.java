/**
 * 
 */
package com.perelens.simulation.risk;

import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventType;
import com.perelens.engine.core.AbstractEventEvaluator;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.Function;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * This function represents a control that can completely or partially mitigate the magnitude of a given event.
 * 
 * @author Steve Branda
 *
 */
public class MitigatingControl extends AbstractEventEvaluator implements Function, EventFilter {

	public enum CONFIG_KEYS implements ConfigKey{
		MC_TO_CONTROL,
		MC_SUCCESS_RATE,
		MC_MITIGATION_FACTOR;
	}
	
	private EventType toControl;
	private double successRate;
	private Distribution mitigationFactor;
	private RandomGenerator succRand = RandomGenerator.NULL_GENERATOR;
	private RandomGenerator mitiRand = RandomGenerator.NULL_GENERATOR;
	
	/**
	 * 
	 * @param id
	 * @param toControl
	 * @param successRate
	 * @param mitigationFactor - the percentage by which the magnitude of the controlled event will be reduced
	 */
	public MitigatingControl(String id, EventType toControl, double successRate, Distribution mitigationFactor) {
		super(id);
		Utils.checkNull(toControl);
		Utils.checkPercentage(successRate);
		Utils.checkNull(mitigationFactor);
		
		this.toControl = toControl;
		this.successRate = successRate;
		this.mitigationFactor = mitigationFactor;
	}

	@Override
	public void initiate(FunctionInfo info) {
		if (successRate < 1.0 && successRate > 0.0) {
			succRand = info.getRandomGenerator();
		}
		
		if (mitigationFactor != RiskUtils.ZERO_DISTRIBUTION && mitigationFactor != RiskUtils.ONE_DISTRIBUTION) {
			mitiRand = info.getRandomGenerator();
		}
	}
	
	
	//Using a filter is more efficient than gating the event type in the process method
	@Override
	public EventFilter getEventFilter() {
		return this;
	}
	@Override
	public boolean filter(Event event) {
		return event.getType() == toControl;
	}

	@Override
	protected void process(Event curEvent) {
		//Original magnitude
		double mag = curEvent.getMagnitude().magnitude();
		
		//See if the control is successful
		double ur = succRand.nextDouble();
		if (Utils.processIsSuccessful(successRate, ur)) {
			ur = mitiRand.nextDouble();
			double mf = mitigationFactor.sample(ur);
			double reduction = mag * mf;
			mag = mag - reduction;
		}
		
		//Raise another event with the new magnitude
		RiskSimEvent toRaise = new RiskSimEvent(this.getId(),curEvent.getType(),curEvent.getTime(),getNextOrdinal(),curEvent);
		toRaise.setMagnitude(mag, curEvent.getMagnitude().unit());
		
		this.raiseEvent(toRaise);
	}
	
	@Override
	public MitigatingControl copy() {
		var toReturn = new MitigatingControl(getId(),toControl,successRate,mitigationFactor);
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(MitigatingControl toSync) {
		super.syncInternalState(toSync);
		toSync.mitigationFactor = mitigationFactor.copy();
		toSync.succRand = succRand.copy();
		toSync.mitiRand = mitiRand.copy();
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		var toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.MC_TO_CONTROL, toControl.name());
		toReturn.put(CONFIG_KEYS.MC_SUCCESS_RATE, Double.toString(successRate));
		toReturn.put(CONFIG_KEYS.MC_MITIGATION_FACTOR, mitigationFactor.getSetup());
		return toReturn;
	}
	
	

}
