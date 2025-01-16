/**
 * 
 */
package com.perelens.simulation.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventFilter;
import com.perelens.engine.api.EventGenerator;
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
   
   This is the general purpose ResourcePool implementation that works for all use cases.
   
 * @author Steve Branda
 *
 */
public class CoreResourcePool extends RequestQueueAndMap implements ResourcePool {

	private static final EventFilter EXCLUSIVE_FILTER = new EventFilter() {
		@Override
		public boolean filter(Event event) {
			return event.getType() instanceof ResourcePoolEvent;
		}
	};
	
	private static final ResPoolEvent NEEDS_RENEW = new ResPoolEvent("needs renew",ResourcePoolEvent.RP_RENEW,0,1);
	private static final ResPoolEvent GRANTED = new ResPoolEvent("granted",ResourcePoolEvent.RP_GRANT,0,1);
	
	public static enum CONFIG_KEYS implements ConfigKey{
		CRP_LIMIT;
	}
	
	private int limit;
	private int granted = 0;
	
	public CoreResourcePool(String id, int limit) {
		super(id);
		if (limit < 1) {
			throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(limit));
		}
		this.limit = limit;
	}

	@Override
	public EventGenerator copy() {
		CoreResourcePool toReturn = new CoreResourcePool(getId(),limit);
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(CoreResourcePool toSync) {
		super.syncInternalState(toSync);
		toSync.granted = this.granted;
		toSync.limit = this.limit;
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		Map<ConfigKey, String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.CRP_LIMIT, Integer.toString(limit));
		return toReturn;
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
	protected void process(Event curEvent) {
		if (curEvent != null) {
			if (curEvent.getType() == ResourcePoolEvent.RP_REQUEST) {
				if (this.m_put(curEvent.getProducerId(), curEvent) == null) {
					this.r_enqueue(curEvent.getProducerId());
				}else {
					throw new IllegalStateException(SimMsgs.requestedBeforeReturned(curEvent.getProducerId()));
				}
			}else if (curEvent.getType() == ResourcePoolEvent.RP_RETURN) {
				if (this.m_remove(curEvent.getProducerId()) == null) {
					throw new IllegalStateException(SimMsgs.falseReturn(curEvent.getProducerId()));
				}
				granted--;
			}else if (curEvent.getType() == ResourcePoolEvent.RP_RENEW) {
				if (this.m_put(curEvent.getProducerId(), curEvent) != NEEDS_RENEW) {
					throw new IllegalStateException(SimMsgs.badState());
				}
			}
		}
		
		//Grant as many requests as possible
		while (granted < limit && this.r_size() > 0) {
			String requestKey = this.r_dequeue();
			Event request = this.m_put(requestKey,GRANTED);
			if (request == NEEDS_RENEW) {
				//Right now the expectation is that any DEFERRED requests will be RENEWED by the requester at the start of the next window
				//Other behavior, like skipping requesters, could be adopted in the future if desired.
				throw new IllegalStateException(SimMsgs.resourceRenewRequired(requestKey));
			}
			Event eg = new ResPoolEvent(getId(),ResourcePoolEvent.RP_GRANT,getTimeProcessed(),getNextOrdinal(),ResourcePoolEvent.GRANT_RESPONSE_TYPES);
			raiseResponse(eg, request);
			super.waitForResponse();
			granted++;
		}	
	}
	
	@Override
	protected void postProcess() {
		//If any requests that arrived during this time window were not granted, then send a deferred event so the function
		//knows it needs to wait at least until the next time window to start repair activities.
		if (this.r_size() > 0) {
			if (this.m_size() ==0) {
				throw new IllegalStateException(SimMsgs.badState());
			}
			for(Iterator<Map.Entry<String,Event>> iter = this.m_iterator(); iter.hasNext();) {
				Map.Entry<String,Event> ent = iter.next();
				Event e = ent.getValue();
				if (e != NEEDS_RENEW && e != GRANTED) {
					Event defer = new ResPoolEvent(getId(),ResourcePoolEvent.RP_DEFER,getTimeProcessed(),getNextOrdinal());
					raiseResponse(defer, e);
					ent.setValue(NEEDS_RENEW);//Clear out the old event
				}
			}
		}
	}

	@Override
	public void initiate(BasicInfo info) {
	}
	
}
