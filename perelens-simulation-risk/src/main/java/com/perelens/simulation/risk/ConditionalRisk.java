/**
 * 
 */
package com.perelens.simulation.risk;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.api.EventSubscriber;
import com.perelens.engine.api.EventType;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.risk.events.RiskEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * This function represents a conditional risk that is evaluated when it receives the indicated trigger event.
 * The conditions can be the presence or absence of an event type other than the trigger event type.
 * If the conditions are met then the output event will be generated with the configured probability.
 * 
 * @author Steve Branda
 *
 */
public class ConditionalRisk extends RealizedRisk implements Comparator<EventType>{

	public static enum CONFIG_KEYS implements ConfigKey{
		CR_CONDITIONS
	}
	
	protected static final int NOT_SET = 0;
	public static final int COND_TRUE = 1;
	public static final int COND_FALSE = 2;
	
	//Implemented as an integer array for efficiency
	//First half stores configuration, second half stores state
	private int[] conditions = null;
	
	private double probability = 1.0;
	private RandomGenerator probGen = RandomGenerator.NULL_GENERATOR;
	
	public ConditionalRisk(String id, EventType trigger, double probability, RiskEvent result) {
		super(id, trigger, result);
		RiskUtils.checkPercentage(probability);
		this.probability = probability;
	}
	
	/**
	 * Adds or updates a conditional state
	 * 
	 * @param condition
	 * @param state
	 */
	public void setCondition(RiskEvent condition, boolean state) {
		RiskUtils.checkRiskEventArg(condition);
		
		if (condition == getTrigger()) {
			throw new IllegalArgumentException(RskMsgs.triggerMayNotBeCondition(condition));
		}
		
		if (conditions == null) {
			conditions = new int[RiskEvent.values().length];
		}
		
		int toSet = state?COND_TRUE:COND_FALSE;
		conditions[condition.ordinal()] = toSet;
		conditions[getConditionStateIndex(condition)] = COND_FALSE;
	}
	
	protected int getConditionConfig(RiskEvent condition) {
		
		if (conditions == null) {
			return NOT_SET;
		}
		
		int offset = getFirstEndIndex();
		int index = condition.ordinal();
		if (index >= offset) {
			//We got passed the END event.  Need to check the index that corresponds to the ordinal of the primary event
			index -= offset;
		}
		return conditions[index];
	}
	
	private int getFirstEndIndex() {
		return RiskEvent.values().length >> 1; //same as divide by 2.  Figure out the index of first END event
	}
	
	protected int getConditionState(RiskEvent condition) {
		int index = getConditionStateIndex(condition);
		return conditions[index];
	}
	
	private int getConditionStateIndex(RiskEvent condition) {
		int offset = getFirstEndIndex();
		int index = condition.ordinal();
		if (index < offset) {
			//We got passed the Main event.  Need to check the index that corresponds to the ordinal of the END event
			index += offset;
		}
		
		return index;
	}
	
	protected void setConditionState(RiskEvent condition, int state) {
		if (state == COND_TRUE || state == COND_FALSE) {
			int index = getConditionStateIndex(condition);
			
			if (conditions[index] == state) {
				throw new IllegalStateException(RskMsgs.badState());
			}
			
			conditions[index] = state;
			
		}else {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public void initiate(FunctionInfo info) {
		if (probability < 1.0) {
			probGen = info.getRandomGenerator();
		}
	}

	@Override
	protected void process(Event curEvent) {
		//Manage the state of the conditions
		if (curEvent.getType() != getTrigger()) {
			if (curEvent.getType().name().endsWith(RiskEvent.END)) {
				setConditionState((RiskEvent)curEvent.getType(), COND_FALSE);
			}else {
				setConditionState((RiskEvent)curEvent.getType(), COND_TRUE);
			}
		}else {
			//Event is the trigger event
			//Call the parent implementation if the conditions are met
			if (conditions != null) {
				int stop = getFirstEndIndex();
				for (int i = 0; i < stop; i++) {
					RiskEvent re = RiskEvent.values()[i];
					int config = getConditionConfig(re);
					int state = getConditionState(re);
					if (config != state) {
						//Conditions are not met.  Return without firing result
						return;
					}
				}
			}
			
			//Conditions met so sample the probability and call parent to fire event
			double sample = probGen.nextDouble();
			if (RiskUtils.processIsSuccessful(probability,sample)) {
				super.process(curEvent);
			}
		}
	}

	@Override
	public boolean filter(Event event) {
		
		EventType toCheck = event.getType();
		
		//Interested in the trigger event
		if (toCheck == getTrigger()) {
			return true;
		}
		
		//Interested if it is a configured conditional event
		if (toCheck instanceof RiskEvent) {
			RiskEvent tc = (RiskEvent) toCheck;
			
			return getConditionConfig(tc) != NOT_SET;
		}
		
		return false;
	}

	/**
	 * Need to make sure that events are received in the following order
	 * 
	 * 1. Configured Condition Start Events
	 * 2. Trigger Event
	 * 3. Configured Condition End Events
	 * 
	 */
	@Override
	public Comparator<? extends EventType> getEventTypeComparator() {
		return this;
	}
	@Override
	public int compare(EventType o1, EventType o2) {
		
		//Logic we need for this object
		if (o1 == getTrigger() && o2 instanceof RiskEvent) {
			if (o2.name().endsWith(RiskEvent.END)) {
				return -1;
			}else {
				return 1;
			}
		}else if (o2 == getTrigger() && o1 instanceof RiskEvent) {
			if (o1.name().endsWith(RiskEvent.END)) {
				return 1;
			}else {
				return -1;
			}
		}
		
		//if specific logic does not apply, then use the default
		return EventSubscriber.DEFAULT_COMPARATOR.compare(o1, o2);
	}

	@Override
	public EventGenerator copy() {
		ConditionalRisk toReturn = new ConditionalRisk(getId(),getTrigger(),probability,getResult());
		syncInternalState(toReturn);
		return toReturn;
	}
	protected void syncInternalState(ConditionalRisk toSync) {
		super.syncInternalState(toSync);
		if (conditions != null) {
			toSync.conditions = Arrays.copyOf(conditions, conditions.length);
		}
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		var toReturn = new HashMap<>(super.getConfiguration());
		if (conditions == null) {
			toReturn.put(CONFIG_KEYS.CR_CONDITIONS, "none");
		}else {
			var cc = new StringBuilder();
			int stop = getFirstEndIndex();
			for (int i = 0; i < stop; i++) {
				if (i > 0) {
					cc.append(';').append(' ');
				}
				if (conditions[i] != NOT_SET) {
					cc.append(RiskEvent.values()[i].name()).append('=');
					if (conditions[i] == COND_TRUE) {
						cc.append("COND_TRUE");
					}else {
						cc.append("COND_FALSE");
					}
				}
			}
			toReturn.put(CONFIG_KEYS.CR_CONDITIONS, cc.toString());
		}
		return super.getConfiguration();
	}
}
