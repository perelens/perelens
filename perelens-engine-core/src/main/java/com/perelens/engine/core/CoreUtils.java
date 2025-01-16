/**
 * 
 */
package com.perelens.engine.core;

import java.util.Comparator;

import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventSubscriber;
import com.perelens.engine.api.EventType;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.core.CoreDistributionProvider;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * @author Steve Branda
 *
 */
public class CoreUtils {
	
	public static final Distribution ZERO_DISTRIBUTION;
	public static final Distribution ONE_DISTRIBUTION;
	
	static {
		var cdb = new CoreDistributionProvider();
		ZERO_DISTRIBUTION = cdb.constant(0);
		ONE_DISTRIBUTION = cdb.constant(1.0);
	}
	
	static final SubEntry[] NO_ENTRIES = new SubEntry[0];
	//Comparators for sorting
	static final Comparator<Event> EVENT_COMPARATOR = Comparator.nullsLast(
			(a1, a2) ->{
				if (a1 == a2) {
					return 0;
				}

				int toReturn = Long.compare(a1.getTime(), a2.getTime());
				if (toReturn != 0) {
					return toReturn;
				}

				toReturn = a1.getProducerId().compareTo(a2.getProducerId());
				if (toReturn != 0) {
					return toReturn;
				}
				
				toReturn = Long.compare(a1.getOrdinal(), a2.getOrdinal());
				if (toReturn !=0) {
					return toReturn;
				}

				return EventSubscriber.DEFAULT_COMPARATOR.compare(a1.getType(), a2.getType());

			}
			);
	
	public static Comparator<Event> getEventComparator(Comparator<? extends EventType> etComp) {
		Utils.checkNull(etComp);
		@SuppressWarnings("unchecked")
		var etCompare = (Comparator<EventType>)etComp;
		if (etCompare == EventSubscriber.DEFAULT_COMPARATOR) {
			return EVENT_COMPARATOR;
		}else {
			return (a1, a2) ->{
				if (a1 == a2) {
					return 0;
				}

				int toReturn = Long.compare(a1.getTime(), a2.getTime());
				if (toReturn != 0) {
					return toReturn;
				}

				toReturn =  etCompare.compare(a1.getType(), a2.getType());
				if (toReturn != 0) {
					return toReturn;
				}
				
				toReturn = a1.getProducerId().compareTo(a2.getProducerId());
				if (toReturn != 0) {
					return toReturn;
				}
				
				return Long.compare(a1.getOrdinal(), a2.getOrdinal());
			};
		}
	}

	static Comparator<SubEntry> ENTRY_COMPARATOR = Comparator.nullsLast(
			(a1,a2) -> {
				if (a1 == a2) {
					return 0;
				}else {
					return a1.getObject().getId().compareTo(a2.getObject().getId());
				}
			}
			);
}
