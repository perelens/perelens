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
import com.perelens.engine.api.EventMagnitude.Unit;
import com.perelens.engine.core.AbstractEventEvaluator;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.Function;
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
   
   
 * Used to generate a single or series of events the represent the impact of a certain "window".
 * A common real world example would be to generate one or more LOSS events whose total magnitude are proportional
 * to a 4 hour service outage demarcated by a FAILED and RETURN_TO_SERVICE event.
 * 
 * @author Steve Branda
 *
 */
public class WindowImpact extends AbstractEventEvaluator implements Function, EventFilter {

	public enum CONFIG_KEYS implements ConfigKey{
		WI_START,
		WI_END,
		WI_SLICE_SIZE,
		WI_IMPACT_EVENT,
		WI_IMPACT_UNIT,
		WI_IMPACT_MAGNITUDE;
	}
	
	private EventType start;
	private EventType end;
	private EventType impact;
	private Unit impactUnit = null;
	private long sliceSize = 0;
	
	private Event curWinStart = null;
	private Distribution impactDist = RiskUtils.ZERO_DISTRIBUTION;
	private RandomGenerator impactGen = RandomGenerator.NULL_GENERATOR;
	private long nextImpactTime = -1;
	
	public WindowImpact(String id, EventType winStart, EventType winEnd, EventType impact, long sliceSize) {
		super(id);
		RiskUtils.checkNull(winStart);
		RiskUtils.checkNull(winEnd);
		RiskUtils.checkNull(impact);
		RiskUtils.checkArgStrictlyPositive(sliceSize);
		
		if (impact instanceof RiskEvent) {
			RiskUtils.checkRiskEventArg((RiskEvent) impact);
		}
		
		if (winStart == winEnd) {
			throw new IllegalArgumentException(RskMsgs.startAndEndEventNotSame(winStart));
		}
		
		start = winStart;
		end = winEnd;
		this.impact = impact;
		this.sliceSize = sliceSize;
	}
	
	public void setMagnitude(Unit impactUnit, Distribution impactPerTimeSlice) {
		RiskUtils.checkNull(impactUnit);
		RiskUtils.checkNull(impactPerTimeSlice);
		
		this.impactUnit = impactUnit;
		this.impactDist = impactPerTimeSlice;
	}

	@Override
	public EventGenerator copy() {
		var toReturn = new WindowImpact(getId(),start,end,impact,sliceSize);
		if (impactUnit != null) {
			toReturn.setMagnitude(impactUnit, impactDist.copy());
		}
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(WindowImpact toSync) {
		super.syncInternalState(toSync);
		toSync.curWinStart = getCurWinStart();
		toSync.nextImpactTime = getNextImpactTime();
		toSync.impactGen = impactGen.copy();
	}

	@Override
	public boolean filter(Event event) {
		return event.getType() == start || event.getType() == end;
	}

	@Override
	public void initiate(FunctionInfo info) {
		if (impactDist != RiskUtils.ZERO_DISTRIBUTION) {
			impactGen = info.getRandomGenerator();
		}
	}

	@Override
	protected void process(Event curEvent) {
		boolean raised = false;
		if (curWinStart != null && nextImpactTime == getTimeProcessed()) {
			raiseImpact();
			raised = true;
		}
		
		if (curEvent != null) {
			if (curEvent.getType() == start) {
				if (curWinStart != null) {
					System.out.println(curWinStart);
					System.out.println();
					System.out.println(curEvent);
					throw new IllegalStateException(RskMsgs.newWindowStartBeforePreviousEnds());
				}

				curWinStart = curEvent;
				incrNextImpactTime();
			}else if (curEvent.getType() == end) {
				if (curWinStart == null) {
					//TODO maybe undo this for dynamic simulations in the future
					throw new IllegalStateException(RskMsgs.badState());
				}else {
					//Window is closing
					if (getTimeProcessed() < nextImpactTime) {
						//raise a partial impact
						raiseImpact();
						raised = true;
					}
					curWinStart = null;
				}
			}else {
				throw new IllegalStateException(RskMsgs.badState());
			}
		}
		
		if (raised && curWinStart != null) {
			incrNextImpactTime();
		}
		
	}
	
	protected long getNextImpactTime() {
		return nextImpactTime;
	}
	
	protected void incrNextImpactTime() {
		nextImpactTime = getTimeProcessed() + sliceSize;
		this.registerCallbackTime(nextImpactTime);
	}
	
	protected Event getCurWinStart() {
		return curWinStart;
	}
	
	protected void raiseImpact() {
		var toRaise = new RiskSimEvent(getId(),impact,getTimeProcessed(),getNextOrdinal(),getCurWinStart());
		if (impactDist != RiskUtils.ZERO_DISTRIBUTION) {
			double adj = 1.0;
			if (getTimeProcessed() < getNextImpactTime()) {
				//partial slice so compute adjustment
				long prevImpactTime = nextImpactTime - sliceSize;
				double duration = getTimeProcessed() - prevImpactTime;
				adj = duration/sliceSize;
			}
			
			double ur = impactGen.nextDouble();
			double imp = impactDist.sample(ur);
			imp = imp * adj;
			toRaise.setMagnitude(imp, impactUnit);
		}
		
		raiseEvent(toRaise);
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		var toReturn = new HashMap<ConfigKey,String>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.WI_START, start.name());
		toReturn.put(CONFIG_KEYS.WI_END, end.name());
		toReturn.put(CONFIG_KEYS.WI_SLICE_SIZE, Long.toString(sliceSize));
		toReturn.put(CONFIG_KEYS.WI_IMPACT_EVENT, impact.name());
		toReturn.put(CONFIG_KEYS.WI_IMPACT_UNIT, impactUnit==null?"none":impactUnit.name());
		toReturn.put(CONFIG_KEYS.WI_IMPACT_MAGNITUDE, impactDist.getSetup());
		return toReturn;
	}
}
