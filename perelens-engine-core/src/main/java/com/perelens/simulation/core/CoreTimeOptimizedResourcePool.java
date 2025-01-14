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
import com.perelens.engine.core.TimeQueue;
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
public class CoreTimeOptimizedResourcePool extends TimeQueue implements ResourcePool{


	private static final EventFilter EXCLUSIVE_FILTER = new EventFilter() {
		@Override
		public boolean filter(Event event) {
			return event.getType() instanceof ResourcePoolEvent;
		}
	};
	
	private final String id;
	private int limit;
	private long ordinal = 1;
	
	public static enum CONFIG_KEYS implements ConfigKey{
		CRP_LIMIT;
	}
	
	public CoreTimeOptimizedResourcePool(String id, int limit) {
		Utils.checkId(id);
		this.id = id;
		
		if (limit < 1) {
			throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(limit));
		}
		this.limit = limit;
		setInitialCapacity(limit);
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

	@Override
	public void consume(long timeWindow, ResponderResources resources) {
		for (Event e : resources.getEvents()) {
			if (e.getType() == ResourcePoolEvent.RP_REQUEST) {
				long timeNeeded = e.getTimeOptimization();
				if (timeNeeded > 0){
					long curTime = e.getTime();
					
					//Clear out any expired time records
					for (long nextAvail = this.tc_peek(); nextAvail > -1 && nextAvail <= curTime; nextAvail = this.tc_peek()) {
						this.tc_dequeue(); 
					}
					
					
					long timeWhenUsable = curTime;
					if (this.tc_size() == limit) {
						//There are no unused resources so get the earliest resource availability
						timeWhenUsable = tc_dequeue();
					}
					
					ResPoolEvent eg = new ResPoolEvent(getId(),ResourcePoolEvent.RP_GRANT,curTime,getNextOrdinal());
					eg.setTimeOptimization(timeWhenUsable);
					System.out.println(e.getProducerId());
					System.out.println(eg); //TODO Remove
					resources.raiseResponse(eg, e);
					
					long timeToAdd = timeWhenUsable + timeNeeded;
					this.tc_enqueue(timeToAdd);
					
					//Sanity check
					if (this.tc_size() > limit) {
						throw new IllegalStateException();
					}
				}else {
					throw new IllegalStateException();
				}
			}else {
				throw new IllegalStateException();
			}
		}
		
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
		System.out.println(info.getDependencies());
	}
}
