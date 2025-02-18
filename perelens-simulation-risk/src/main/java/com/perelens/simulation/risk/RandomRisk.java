/**
 * 
 */
package com.perelens.simulation.risk;

import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.Function;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.mixed.AbstractEventWindow;
import com.perelens.simulation.risk.events.RiskEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * This function represents a risk that is realized based on some underlying random system.
 * 
 * @author Steve Branda
 *
 */
public class RandomRisk extends AbstractEventWindow implements Function{

	public static enum CONFIG_KEYS implements ConfigKey{
		RR_EVENT_ARRIVAL_DIST;
	}
	
	private Distribution arrivalTime;
	private RandomGenerator arrivalGen;
	
	public RandomRisk(String id, Distribution eventArrivalTime, RiskEvent toGenerate) {
		super(id,toGenerate,RiskUtils.getEndEvent(toGenerate));
		
		RiskUtils.checkNull(eventArrivalTime);
		this.arrivalTime = eventArrivalTime;
	}
	
	@Override
	public void initiate(FunctionInfo info) {
		super.initiate(info);
		arrivalGen = info.getRandomGenerator();
	}

	@Override
	public RandomRisk copy() {
		//Distribution will be copied in syncInternalState call
		RandomRisk toReturn = new RandomRisk(this.getId(), arrivalTime, (RiskEvent)getWindowStartEvent());
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(RandomRisk toSync) {
		super.syncInternalState(toSync);
		toSync.arrivalGen = arrivalGen == null?null:arrivalGen.copy();
	}
	
	protected void setEventArrival() {
		double ur = arrivalGen.nextDouble();
		double ar = arrivalTime.sample(ur);
		
		long nextArrivalOffset = Math.max((long)ar, 1); //Must advance time at least one unit
		setNextEventStart(getTimeProcessed() + nextArrivalOffset);
		this.registerCallbackTime(getNextEventStart());
	}
	
	@Override
	protected void preProcess() {
		super.preProcess();
		if (this.getTimeProcessed() == 0) {
			//Only happens the first time the function is invoked during the simulation
			//Doing this here instead of the constructor allows the random distribution sampling to be done in parallel across all the functions
			setEventArrival();
		}
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		HashMap<ConfigKey, String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.RR_EVENT_ARRIVAL_DIST, arrivalTime.getSetup());
		return toReturn;
	}

	@Override
	protected void windowEnded() {
		setEventArrival();
	}

	@Override
	protected Event createEvent(String id, EventType type, long time, long ordinal, Event curEvent) {
		if (curEvent != null) {
			throw new IllegalStateException(RskMsgs.badState());
		}
		return new RiskSimEvent(id,type,time,ordinal);
	}

}
