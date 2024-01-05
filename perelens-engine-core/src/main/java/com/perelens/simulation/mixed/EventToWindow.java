/**
 * 
 */
package com.perelens.simulation.mixed;

import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventGenerator;
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
   
   
 * Mixed mode event translator that translates from a single trigger event to a window demarcated
 * by the passed start and end types.
 * 
 * @author SteveBranda
 *
 */
public class EventToWindow extends AbstractEventWindow implements EventFilter {

	
	private EventType trigger;
	
	public EventToWindow(String id, EventType trigger, EventType winStartEvent, EventType winEndEvent) {
		super(id,winStartEvent,winEndEvent);
		Utils.checkNull(trigger);
		
		this.trigger = trigger;
	}
	
	@Override
	public EventGenerator copy() {
		var tr = new EventToWindow(getId(),trigger,getWindowStartEvent(),getWindowEndEvent());
		super.syncInternalState(tr);
		return tr;
	}

	@Override
	public boolean filter(Event event) {
		return event.getType() == trigger;
	}
	
	@Override
	public EventFilter getEventFilter() {
		return this;
	}
	
	protected EventType getTrigger() {
		return trigger;
	}

	@Override
	protected void process(Event curEvent) {
		if (curEvent != null) {
			if (getTimeProcessed() <= this.getNextEventEnd()) {
				//Received a second trigger during an existing window
				//Just see if the closing time should be extended
				setEventEnd();
			}else {
				this.setNextEventStart(curEvent.getTime());
			}
		}
		super.process(curEvent);
	}

	@Override
	protected Event createEvent(String id, EventType type, long time, long ordinal, Event curEvent) {
		if (curEvent == null) {
			return new SimEvent(type,id,time,ordinal);
		}else {
			return new SimEvent(type,id,time,ordinal,curEvent);
		}
	}

	@Override
	protected void windowEnded() {
	}

}
