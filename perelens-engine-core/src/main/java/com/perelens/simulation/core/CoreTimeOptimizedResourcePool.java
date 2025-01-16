/**
 * 
 */
package com.perelens.simulation.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.api.ResponderResources;
import com.perelens.engine.core.CoreUtils;
import com.perelens.engine.core.TimePlusEventQueue;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.BasicInfo;
import com.perelens.simulation.api.ResourcePool;
import com.perelens.simulation.events.ResourcePoolEvent;

/**
 * Copyright 2020-2024 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   ResourcePool implementation that only works with Time Optimized ResourcePoolEvents.
   Requesters can use time optimization when they know exactly how many time units they will hold the resource for before returning it.
   Given this information the ResourcePool can return a time optimized GRANT event that tells the requester the time cycle at which the resource can be used.
   This eliminates the need for RETURN and RENEW events to be passed, reducing the overhead of the ResourcePool interaction by at least half.
   
 * @author Steve Branda 
 */
public class CoreTimeOptimizedResourcePool extends TimePlusEventQueue implements ResourcePool{


	private static final EventFilter EXCLUSIVE_FILTER = new EventFilter() {
		@Override
		public boolean filter(Event event) {
			return event.getType() instanceof ResourcePoolEvent;
		}
	};
	
	private final String id;
	private int limit;
	private long ordinal = 1;
	private int dependencyCount = 0;
	private final double scaleFactor;
	
	public static enum CONFIG_KEYS implements ConfigKey{
		CRP_LIMIT;
	}
	
	/**
	 * Initializes a ResourcePool with a scaleFactor of zero, which is the most conservative and accurate value.
	 * 
	 * @param id
	 * @param limit - Number of total resources to grant before requests start to queue up
	 */
	public CoreTimeOptimizedResourcePool(String id, int limit) {
		this(id,limit,0.0);
	}
	
	/**
	 * Initializes a ResourcePool with a customized scaleFactor.
	 * A scale factor of 0.0 will ensure that no events get processed out of order
	 * A scale factor of 1.0 will remove all protection against events getting processed out of order.
	 * A scale factor of 0.5 will begin ensuring that no events get processed out of order once 50% of the limit has been reached.
	 * 
	 * @param id
	 * @param limit	- Number of total resources to grant before requests start to queue up
	 * @param scaleFactor - double value between 0.0 and 1.0
	 */
	public CoreTimeOptimizedResourcePool(String id, int limit, double scaleFactor) {
		Utils.checkId(id);
		Utils.checkPercentage(scaleFactor);
		this.id = id;
		
		if (limit < 1) {
			throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(limit));
		}
		this.limit = limit;
		setInitialCapacity(limit);
		this.scaleFactor = scaleFactor; //Invert the value
		this.setComparator(CoreUtils.getEventComparator(this.getEventTypeComparator()));
	}
	
	@Override
	public EventFilter getEventFilter() {
		return EXCLUSIVE_FILTER;
	}
	
	@Override
	public Comparator<ResourcePoolEvent> getEventTypeComparator() {
		//RENEW events must be ordered before REQUEST events if they have the same time
		//Uses ordering of the enum declaration in ResourcePoolEvent
		return (a1,a2)->{
			return a1.compareTo(a2);
		};
	}

	@Override
	public EventGenerator copy() {
		var tr = new CoreTimeOptimizedResourcePool(id,limit);
		return tr;
	}
	
	//EventCutOffTime is the earliest time an out of order request could come in a future method call
	//It will be safe to process any requests with a time before EventCutOffTime
	private long eventCutOffTime = Long.MAX_VALUE;
	@Override
	public void consume(long timeWindow, ResponderResources resources) {
		String firstDep = Utils.EMPTY_STRING;
		eventCutOffTime = Long.MAX_VALUE;
		boolean singleProducer = true;
		
		var eIter = resources.getEvents().iterator();
		
		while(eIter.hasNext() || (this.ev_hasMore() && this.ev_peek().getTime() < eventCutOffTime)) {
			Event e = null;
			
			if (eIter.hasNext()) {
				e = eIter.next();
			}
			
			if (this.ev_hasMore()) {
				if (e == null) {
					e = this.ev_dequeue();
				}else if (e.getTime() >= this.ev_peek().getTime()) {
					this.ev_enqueue(e);
					e = this.ev_dequeue();
				}
			}
			
			if (e.getType() == ResourcePoolEvent.RP_REQUEST) {
				long curTime = e.getTime();
				
				//Clear out any expired time records
				for (long nextAvail = this.tc_peek(); nextAvail > -1 && nextAvail <= curTime; nextAvail = this.tc_peek()) {
					this.tc_dequeue(); 
				}
				
				long timeNeeded = e.getTimeOptimization();
				if (timeNeeded == Event.NOT_TIME_OPTIMIZED){
					throw new IllegalStateException();
				}

				if (limit >= dependencyCount) {
					//Don't need to worry about queuing due to capacity being large enough that the pool isn't contended
					grantResourceRequest(e,curTime,timeNeeded,resources);
				}else{
					/*
					 * Need to worry about queuing.
					 * For a contended ResourcePool it is possible that Events will arrive out of order due to time over multiple calls.
					 * For example, the first time this method is called during the time window the events might be:
					 * 		ProducerA - RP_Request at time 100
					 * 		ProducerB - RP_Request at time 500
					 * Then the second time it is called during the window the events might be:
					 * 		ProducerA - RP_Request at time 300
					 * 		ProducerB - RP_Request at time 600
					 * 
					 * Processing the ProducerB time 500 event during the first method call can cause problems when the ResourcePool is contended.
					 * If the ResourcePool has a limit of 1, and it processes the ProducerB event during the first method call then the ProducerA event at
					 * time 300 will have have to wait until after time 500 to get a resource when in reality it should have received the resource before
					 * Producer B.
					 * */
					
					if (singleProducer && !firstDep.equals(e.getProducerId())){
						if (firstDep == Utils.EMPTY_STRING) {
							firstDep = e.getProducerId();
						}else {
							singleProducer = false;
						}
					}
					
					
					if (singleProducer) {
						//Can process as many events as we want until we encounter more than one producer
						grantResourceRequest(e,curTime,timeNeeded,resources);
					}else if (curTime < eventCutOffTime) {
						//Can safely process any event that occurs before the cut off time
						grantResourceRequest(e,curTime,timeNeeded,resources);
					}else if (this.tc_size() < limit * scaleFactor) {
						//Unsafely process events up to the limit specified by the scale factor
						grantResourceRequest(e,curTime,timeNeeded,resources);
					}else {
						//Need to queue events and try to process next call
						this.ev_enqueue(e);
					}
				}
			}else {
				throw new IllegalStateException();
			}
			
			//Sanity check
			if (this.tc_size() > limit) {
				throw new IllegalStateException();
			}
		}	
		
		if (this.ev_hasMore()) {
			//Need to make sure this EventResponder gets called again until the Event queue is drained
			//TODO figure out if this is making things slow
			resources.keepActive();
		}
	}
	
	private void grantResourceRequest(Event inResponseTo, long curTime, long timeNeeded, ResponderResources resources) {
		long timeWhenUsable = curTime;
		if (this.tc_size() == limit) {
			//There are no unused resources so get the earliest resource availability
			timeWhenUsable = tc_dequeue();
		}
		
		long timeToAdd = timeWhenUsable + timeNeeded;
		ResPoolEvent eg = new ResPoolEvent(getId(),ResourcePoolEvent.RP_GRANT,curTime,getNextOrdinal());
		eg.setTimeOptimization(timeWhenUsable);
		resources.raiseResponse(eg, inResponseTo);
		if (timeToAdd < eventCutOffTime) {
			eventCutOffTime = timeToAdd;
		}

		this.tc_enqueue(timeToAdd);
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		Map<ConfigKey, String> toReturn = new HashMap<>();
		toReturn.put(CONFIG_KEYS.CRP_LIMIT, Integer.toString(limit));
		return toReturn;
	}

	@Override
	public String getId() {
		return id;
	}
	
	protected long getNextOrdinal() {
		return ordinal++;
	}
	
	@Override
	public void initiate(BasicInfo info) {
		dependencyCount = info.getDependencies().size();
	}
}
