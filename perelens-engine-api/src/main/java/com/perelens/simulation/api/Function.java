/**
 * 
 */
package com.perelens.simulation.api;

import com.perelens.engine.api.EventEvaluator;

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
 * @author Steve Branda
 *
 */
public interface Function extends EventEvaluator {

	/**
	 * Called by the simulation engine before a simulation begins to execute.
	 * This is the last opportunity for a given function to configure its internal state before simulation begins.
	 * 
	 * @param info
	 */
	public void initiate(FunctionInfo info);
	
}
