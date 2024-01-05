/**
 * 
 */
package com.perelens.engine.api;

import java.util.Comparator;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * @author Steve Branda
 *
 */
public interface EventSubscriber {

	public static final Comparator<EventType> DEFAULT_COMPARATOR = (a1, a2) ->{
		if (a1 == a2) {
			return 0;
		}

		return a1.toString().compareTo(a2.toString());

	};
	
	public String getId();
	
	public default EventFilter getEventFilter() {
		return EventFilter.NULL_FILTER;
	}
	
	public default Comparator<? extends EventType> getEventTypeComparator(){
		return DEFAULT_COMPARATOR;
	}
}
