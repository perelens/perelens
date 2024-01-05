/**
 * 
 */
package com.perelens.simulation.mixed;

import java.util.HashMap;
import java.util.Map;

import com.perelens.Msgs;
import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventType;
import com.perelens.engine.core.AbstractEventEvaluator;
import com.perelens.engine.core.CoreUtils;
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
   
 * This function represents a risk that is realized based on some underlying random system.
 * 
 * @author Steve Branda
 *
 */
public abstract class AbstractEventWindow extends AbstractEventEvaluator implements Function{

	public static enum CONFIG_KEYS implements ConfigKey{
		EW_EVENT_ARRIVAL_DIST,
		EW_EVENT_DURATION_DIST;
	}
	
	private RandomGenerator durationGen = RandomGenerator.NULL_GENERATOR;
	private Distribution eventDuration = CoreUtils.ZERO_DISTRIBUTION;
	private EventType winStart;
	private EventType winEnd;
	
	private long nextEventStart = -1;
	private long nextEventEnd = -1;
	
	protected AbstractEventWindow(String id, EventType start, EventType end) {
		super(id);
		Utils.checkNull(start);
		Utils.checkNull(end);
		
		winStart = start;
		winEnd = end;
	}
	
	public void setEventDuration(Distribution eventDuration) {
		Utils.checkNull(eventDuration);
		this.eventDuration = eventDuration;
	}
	
	
	@Override
	public void initiate(FunctionInfo info) {
		if (eventDuration != CoreUtils.ZERO_DISTRIBUTION) {
			durationGen = info.getRandomGenerator();
		}
	}

	protected void syncInternalState(AbstractEventWindow toSync) {
		super.syncInternalState(toSync);
		toSync.setEventDuration(eventDuration.copy());
		toSync.durationGen = durationGen == null?null:durationGen.copy();
	}
	
	
	protected void setEventEnd() {
		double ur = durationGen.nextDouble();
		double er = eventDuration.sample(ur);
		
		long nextEndOffset = (long)Math.round(er);
		
		if (getTimeProcessed() < getNextEventEnd()) {
			//Trigger arrival during an active window
			//Sample another end time and extend the window if necessary
			long target = getTimeProcessed() + nextEndOffset;
			if (target > getNextEventEnd()) {
				setNextEventEnd(target);
				this.registerCallbackTime(target);
			}
		}else {
			//End events can arrive at the same time as the arrival event.
			//This normally happens during simulations where state duration is not interesting and the END events will be ignored
			setNextEventEnd(getTimeProcessed() + nextEndOffset);
			if (nextEndOffset > 0) {
				this.registerCallbackTime(getNextEventEnd());
			}
		}
	}
	
	private void setNextEventEnd(long time) {
		
		if (time <= nextEventEnd) {
			throw new IllegalArgumentException(Msgs.timeMustAdvance(nextEventEnd, time));
		}
		
		nextEventEnd = time;
	}
	
	protected long getNextEventEnd() {
		return nextEventEnd;
	}
	
	protected void setNextEventStart(long time) {
		if (time <= nextEventStart) {
			throw new IllegalArgumentException(Msgs.timeMustAdvance(nextEventStart, time));
		}
		
		nextEventStart = time;
		if (nextEventStart > getTimeProcessed()) {
			this.registerCallbackTime(nextEventStart);
		}
	}
	
	protected EventType getWindowStartEvent() {
		return winStart;
	}
	
	protected EventType getWindowEndEvent() {
		return winEnd;
	}
	
	protected long getNextEventStart() {
		return nextEventStart;
	}
	
	@Override
	protected void process(Event curEvent) {
		if (getNextEventStart() == getTimeProcessed()) {
			//Raise the start of the event
			Event toRaise = createEvent(this.getId(),winStart,getNextEventStart(),getNextOrdinal(), curEvent);
			this.raiseEvent(toRaise);
			
			setEventEnd();
		}
		
		if (getNextEventEnd() == getTimeProcessed()) {
			//Raise the end of the event
			Event toRaise = createEvent(this.getId(),winEnd,getNextEventEnd(),getNextOrdinal(),curEvent);
			this.raiseEvent(toRaise);
			
			windowEnded();
		}
	}
	
	protected abstract Event createEvent(String id, EventType type, long time, long ordinal, Event curEvent);
	
	protected abstract void windowEnded();

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		HashMap<ConfigKey, String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.EW_EVENT_DURATION_DIST, eventDuration.getSetup());
		return toReturn;
	}

}
