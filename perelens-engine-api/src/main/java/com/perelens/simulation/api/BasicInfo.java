/**
 * 
 */
package com.perelens.simulation.api;

import java.util.Set;

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
public interface BasicInfo {

	/**
	 * Return the ids of the functional dependencies that this object may see events from.
	 * 
	 * @return
	 */
	public Set<String> getDependencies();
	
	/**
	 * Returns the TimeTranslator function for this {@link Simulation}
	 * 
	 * @return
	 */
	public TimeTranslator getTimeTranslator();
	
	/**
	 * Returns a RandomNumber generator instance the function can use to generate a series of random numbers.
	 * 
	 * @return
	 */
	public RandomGenerator getRandomGenerator();
	
}
