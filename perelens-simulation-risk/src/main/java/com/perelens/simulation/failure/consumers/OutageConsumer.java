/**
 * 
 */
package com.perelens.simulation.failure.consumers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import com.perelens.engine.api.Event;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.failure.events.FailureSimulationEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Consumer that collects outage durations and detailed record of a configurable number of the longest duration outages
 * 
 * @author Steve Branda
 *
 */
public class OutageConsumer extends AbstractFailureConsumer {

	protected static final Outage[] EMPTY_OUTAGES = new Outage[0];
	
	public static class Outage {
		
		private Event startEvent;
		private Event endEvent;
		
		protected Outage(Event startEvent, Event endEvent) {
			this.startEvent = startEvent;
			this.endEvent = endEvent;
		}
		
		public Event getStart() {
			return startEvent;
		}
		
		public Event getEnd() {
			return endEvent;
		}

	}
	
	private long[] downtimeDuration = new long[1024];
	private int ddIndex = 0;
	private Outage[] topOutages;
	private boolean ignoreFailingOver = false;
	private long ignoreLessThan = 0;
	
	public OutageConsumer(String id, int topCount) {
		super(id);
		if (topCount < 0) {
			throw new IllegalArgumentException();
		}
		if (topCount > 0) {
			topOutages = new Outage[topCount + 1];
		}else {
			topOutages = EMPTY_OUTAGES;
		}
	}
	
	public boolean isIgnoreFailingOver() {
		return ignoreFailingOver;
	}
	
	public void setIgnoreFailingOver(boolean value){
		ignoreFailingOver = value;
	}
	
	public void setIgnoreLessThan(long threshold) {
		if (threshold < 0) {
			throw new IllegalArgumentException();
		}
		ignoreLessThan = threshold;
	}
	
	public long[] getDowntimeDurations() {
		return execute(()->{
			long [] toReturn = new long[ddIndex];
			System.arraycopy(downtimeDuration, 0, toReturn, 0, ddIndex);
			return toReturn;
		});
	}
	
	public Outage[] getTopOutages() {
		if (topOutages == EMPTY_OUTAGES) {
			return EMPTY_OUTAGES;
		}
		
		
		return execute(()->  {
			//Find the first index with an outage record
			int start = -1;
			for (int i = 1; i < topOutages.length; i++) {
				if (topOutages[i] != null) {
					start = i;
					break;
				}
			}

			if (start == -1) {
				return EMPTY_OUTAGES;
			}else {
				Outage[] toReturn = new Outage[topOutages.length - start];
				System.arraycopy(topOutages, start, toReturn, 0, toReturn.length);
				return toReturn;
			}
		});
	}
	
	public static final Comparator<Outage> OUTAGE_COMPARATOR;
	static {
		Comparator<Outage> temp = (a,b) -> {
			long aDur = a.getEnd().getTime() - a.getStart().getTime();
			long bDur = b.getEnd().getTime() - b.getStart().getTime();
			
			return Long.compare(aDur, bDur);
		};
		
		OUTAGE_COMPARATOR = Comparator.nullsFirst(temp);
	}
		
	@Override
	protected void processChange(Event currentEvent) {
		if (!isFailed() && getLastEvent() != null) {
			
			if (isIgnoreFailingOver()) {
				/** 
				 * Check to see if the outage is just a result of Fail Over Activity, which means the following:
				 * 		-All branches of the event tree from the initial FS_FAILED event are rooted in a FAILING_OVER event
				 * 		-The only producers "causing" the RETURN_TO_SERVICE event were all original "causes" of the FAILING_OVER event/s
				 * 		-The RETURN_TO_SERVICE event tree does not contain additional FS_FAILED events that would have extended the outage window
				 */
				Event startEvent = getLastEvent();
				HashSet<String> failoverCauses = new HashSet<>();
				
				boolean justFailingOver = isJustFailingOver(false,startEvent,failoverCauses);
				if (justFailingOver) {
					boolean noExtraInfoInRepair = true;
					
					//Check to see if the only producers "causing" to the RETURN_TO_SERVICE event
					//were all original "causes" of the FAILING_OVER event
					for (Iterator<Event> i = currentEvent.causedBy(); i.hasNext();) {
						Event cur = i.next();
						if (cur.getType() != FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
							noExtraInfoInRepair = false;
							break;
						}
						if (!failoverCauses.contains(cur.getProducerId())) {
							noExtraInfoInRepair = false;
							break;
						}
					}
					
					if (noExtraInfoInRepair) {
						return; //Filter out as a failing over event
					}
				}
			}
			
			
			long duration = currentEvent.getTime() - getLastEvent().getTime();
			
			if (duration < ignoreLessThan) {
				return; //Filter out short outage
			}
			
			downtimeDuration = Utils.append(downtimeDuration, duration, ddIndex);
			ddIndex++;
			
			if (topOutages != EMPTY_OUTAGES) {
				topOutages[0] = new Outage(getLastEvent(),currentEvent);
				Arrays.sort(topOutages,OUTAGE_COMPARATOR);
				topOutages[0] = null;
			}	
		}
	}
	
	private boolean isJustFailingOver(boolean belowFailingOver, Event cur, HashSet<String> foCauses) {
		
		boolean curIsFo = cur.getType() == FailureSimulationEvent.FS_FAILING_OVER;
		Iterator<Event> causes = cur.causedBy();
		
		foCauses.add(cur.getProducerId());
		
		if (!causes.hasNext()) {
			//We have reached the bottom of a branch so return
			return curIsFo || belowFailingOver;
		}
		
		//Check each branch starting from this event
		boolean toReturn = true;
		do {
			toReturn = toReturn && isJustFailingOver(curIsFo || belowFailingOver, causes.next(), foCauses);
		}while(toReturn && causes.hasNext());
		
		return toReturn;
	}
}
