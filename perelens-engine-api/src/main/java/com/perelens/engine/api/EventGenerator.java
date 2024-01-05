/**
 * 
 */
package com.perelens.engine.api;

import java.util.Map;

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
public interface EventGenerator extends EventSubscriber {

	public static enum CONFIG_KEYS implements ConfigKey{
		EG_ID;
	}
	
	/**
	 * This method must return a deep copy of the EventGenerator instance.
	 * This copy must have the exact same state and functionality as the original instance.
	 * Calls to the copy may not affect the state and functionality of the original instance.
	 * These copies are used to resolve circular dependencies between event processing objects when they are detected by the engine.
	 * If this method returns null if called by the engine, then the engine will halt execution with an exception.
	 * 
	 * @return
	 */
	public EventGenerator copy();
	
	/**
	 * This method must return a sorted map will all the stateless configuration values for the EventEvaluator instance.
	 * If two EventEvaluator return identical maps, as determined by {@link java.util.Map#equals(Object)} method, then
	 * the EventEvaluator instances should generate the event stream given the same event inputs and polling
	 * @return
	 */
	public Map<ConfigKey, String> getConfiguration();
}
