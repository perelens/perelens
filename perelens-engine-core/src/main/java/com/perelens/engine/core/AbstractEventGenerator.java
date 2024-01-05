/**
 * 
 */
package com.perelens.engine.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.perelens.Msgs;
import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.api.EventType;
import com.perelens.engine.api.ResponderResources;
import com.perelens.engine.utils.Utils;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Abstract implementation class to ease the implementation burden of Function Code.
 * 
 * @author Steve Branda
 *
 */
public abstract class AbstractEventGenerator<R extends ResponderResources> extends TimePlusEventQueue implements EventGenerator{

	private String id;
	private long timeProcessed = 0;
	private long windowStart = 0;
	private R resources;
	private boolean waitForResponse = false;
	private long ordinal = 1;
	
	@SuppressWarnings("unchecked")
	protected AbstractEventGenerator(String id) {
		Utils.checkId(id);
		this.id = id;
		this.setComparator(CoreUtils.getEventComparator((Comparator<EventType>) this.getEventTypeComparator()));
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	protected long getTimeProcessed() {
		return timeProcessed;
	}
	
	private LinkedList<Event> allEvents = new LinkedList<Event>();
	private void setTimeProcessed(Event ev) {
		allEvents.add(ev);
		if (allEvents.size() > 100) {
			allEvents.removeFirst();
		}
		setTimeProcessed(ev.getTime());
	}
	
	private void setTimeProcessed(long completed) {
		if (completed < timeProcessed) {
			throw new IllegalArgumentException(EngineMsgs.timeMustAdvance(timeProcessed,completed));
		}
		timeProcessed = completed;
	}
	
	protected void registerCallbackTime(long time) {
		if (time < timeProcessed) {
			throw new IllegalArgumentException(EngineMsgs.timeMustAdvance(timeProcessed,time));
		}
		tc_enqueue(time);
	}
	
	protected void waitForResponse() {
		waitForResponse = true;
	}
	
	protected void syncInternalState(AbstractEventGenerator<R> toSync) {
		Utils.checkNull(toSync);
		super.syncInternalState(toSync);
		toSync.id = this.id;
		toSync.timeProcessed = this.timeProcessed;
	}
	
	protected long getNextOrdinal() {
		return ordinal++;
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		return Collections.singletonMap(EventGenerator.CONFIG_KEYS.EG_ID, getId());
	}
	
	protected abstract void process(Event curEvent);
	protected void preProcess() {};
	protected void postProcess() {};
	
	final public void consume(long timeWindow, R resources) {
		this.resources = resources;
		long eventTimeCutoff = Long.MAX_VALUE;
		
		if (getTimeProcessed() == getWindowStart()) {
			preProcess();
		}
		
		Iterator<Event> events = resources.getEvents().iterator();
		Event curEvent = null;
		
		if(this.ev_hasMore()) {
			//There are queued up events from the previous call so we need to drain the events
			//arriving during this call to consume() into the existing priority queue.
			while(events.hasNext()) {
				Event toEnq = events.next();
				this.ev_enqueue(toEnq);
			}
		}
		
		if (events.hasNext()) {
			curEvent = events.next();
		}else if (this.ev_hasMore()) {
			curEvent = this.ev_dequeue();
		}
		
		do {
			if (waitForResponse) {
				if (curEvent != null && curEvent.getTime() > eventTimeCutoff){
					//Event is beyond the time cutoff so enqueue
					this.ev_enqueue(curEvent);
					curEvent = null;
				}
			}
			
			if (curEvent != null) {
				if (curEvent.getTime() < peekTime()) {
					//The next event happens before the next call back time
					setTimeProcessed(curEvent);
					process(curEvent);
					curEvent = null;
				}else if (curEvent.getTime() == peekTime()) {
					//The next event happens at the same time as the next call back time
					dequeueTime();
					setTimeProcessed(curEvent);
					process(curEvent);
					curEvent = null;
				}else {
					//The next callback must be processed before the next event
					long time = dequeueTime();
					setTimeProcessed(time);
					process(null);
				}
			}
			
			if (waitForResponse && eventTimeCutoff == Long.MAX_VALUE) {
				//The event handling logic needs to wait for a response
				//Process all logic and events up to the current time.
				//Queue any events beyond the current time so they can be processed during the next call to consume()
				eventTimeCutoff = getTimeProcessed(); 
			}
			
			if (curEvent == null) {
				if (events.hasNext()) {
					curEvent = events.next();
				}else if (this.ev_hasMore() && this.ev_peek().getTime() <= eventTimeCutoff) {
					curEvent = this.ev_dequeue();
				}
			}
			
			if (curEvent == null) {
				if (waitForResponse && eventTimeCutoff < peekTime()) {
					//No more events to process, but waiting for a response so stop advancing time and wait for more events to come
					break;
				}else{
					//No more events and we don't expect another call to consume due to a response
					if (timeWindow < peekTime()) {
						//The next callback is in a future time window, so close out this time window
						postProcess();
						setTimeProcessed(timeWindow);
					}else if(timeWindow == peekTime()) {
						//The next callback is the exact end of this time window
						dequeueTime();
						setTimeProcessed(timeWindow);
						process(null);
						postProcess();
					}else {
						long time = dequeueTime();
						setTimeProcessed(time);
						process(null);
					}
				}
			}
		}while(curEvent != null || getTimeProcessed() < timeWindow);
		
		if (!waitForResponse) {
			if (getTimeProcessed() == timeWindow) {
				setWindowStart();
			}else {
				throw new IllegalStateException(Msgs.badState());
			}
		}else {
			waitForResponse = false; //Clear the flag for the next call of consume
		}
		
		this.resources = null;
	}
	
	private long peekTime() {
		if (tc_hasMore()) {
			return tc_peek();
		}else {
			return Long.MAX_VALUE;
		}
	}
	
	private long dequeueTime() {
		long toReturn = tc_dequeue();
		while(tc_hasMore() && tc_peek() == toReturn) {
			tc_dequeue(); //clear out any redundant times
		}
		return toReturn;
	}

	private void setWindowStart() {
		windowStart = getTimeProcessed();
	}
	
	protected long getWindowStart() {
		return windowStart;
	}
	
	protected void raiseResponse(Event toRaise, Event inResponseTo) {
		resources.raiseResponse(toRaise, inResponseTo);
		//Automatically advance time when raising an event
		if (getTimeProcessed() < toRaise.getTime()) {
			setTimeProcessed(toRaise.getTime());
		}else if (getTimeProcessed() > toRaise.getTime()) {
			throw new IllegalStateException(Msgs.badState());
		}
	}
	
	protected void raiseEvent(Event toRaise) {
		//Automatically advance time when raising an event
		if (getTimeProcessed() < toRaise.getTime()) {
			setTimeProcessed(toRaise.getTime());
		}else if (getTimeProcessed() > toRaise.getTime()) {
			throw new IllegalStateException(Msgs.badState());
		}
	}
	
	protected R getResources() {
		return resources;
	}
}
