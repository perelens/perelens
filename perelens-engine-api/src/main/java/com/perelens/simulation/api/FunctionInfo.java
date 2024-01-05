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
 * Carries all the information about a given function that the simulation has access to
 * 
 * @author Steve Branda
 *
 */
public interface FunctionInfo extends BasicInfo {

	/**
	 * Return the ids of the Resource Pools that this function has access to
	 * 
	 * @return
	 */
	public Set<String> getResourcePools();
	
}
