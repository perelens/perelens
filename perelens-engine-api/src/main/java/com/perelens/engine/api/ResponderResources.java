/**
 * 
 */
package com.perelens.engine.api;

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
public interface ResponderResources extends ConsumerResources {

	/**
	 * Used to raise events to a specific EventEvaluator in response to a specific event.
	 * This allows EventEvaluators without standing subscriptions to react to events and impact the simulation.
	 * 
	 * @param toRaise
	 * @param inResponseTo
	 */
	public void raiseResponse(Event toRaise, Event inResponseTo);
	
	
	/**
	 * Keeps a responder active for one more call to consume() within the current time window.
	 * 
	 */
	public void keepActive();
}
