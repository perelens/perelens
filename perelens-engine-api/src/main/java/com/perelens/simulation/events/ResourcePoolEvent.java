/**
 * 
 */
package com.perelens.simulation.events;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.perelens.engine.api.EventType;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * 
 *  Enumeration member names are prefixed with 'RP_' because the name() method should return a unique value.
 *  
 * @author Steve Branda
 *
 */
public enum ResourcePoolEvent implements EventType{
	//The order of the enumeration is important here
	//RENEW must stay the first event type
	RP_RENEW,		//Renew Request after being DEFERRED in the previous window
	RP_RETURN,    	//Requester returns resource to pool
	RP_DEFER,		//Requester defers returning or Pool defers granting resource until at least the next window
	RP_REQUEST, 	//Initial Request for a resource
	RP_GRANT;		//Resource granted to requester
	
	public static final Collection<EventType> REQUEST_RESPONSE_TYPES = Collections.unmodifiableCollection(
			Arrays.asList(new EventType[] {ResourcePoolEvent.RP_GRANT,ResourcePoolEvent.RP_DEFER}));
	
	public static final Collection<EventType> GRANT_RESPONSE_TYPES = Collections.unmodifiableCollection(
			Arrays.asList(new EventType[] {ResourcePoolEvent.RP_RETURN,ResourcePoolEvent.RP_DEFER}));
}
