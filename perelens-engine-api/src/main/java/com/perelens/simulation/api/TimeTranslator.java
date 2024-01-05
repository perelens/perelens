/**
 * 
 */
package com.perelens.simulation.api;

import java.time.Instant;

import com.perelens.engine.api.Event;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * Interface for logic that will translate the current simulation time to a java.time object.
 * Implementations should be safe for concurrent access by multiple threads.
 * 
 * @author Steve Branda
 *
 */
public interface TimeTranslator {
	
	/**
	 * Translates the simulation time of the passed event to a {@link java.time.Instant}
	 * 
	 * @param e
	 * @return
	 */
	public Instant getInstant(Event e);

	
	/**
	 * Returns a deep copy of this TimeTranslator
	 * 
	 * @return
	 */
	public TimeTranslator copy();
	
	public String getSetup();
}
