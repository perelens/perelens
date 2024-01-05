/**
 * 
 */
package com.perelens.simulation.api;

import java.util.Collection;

import com.perelens.engine.api.EventConsumer;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * The executable representation of the simulation.
 * Once instantiated the core configuration cannot change by either adding or removing functions or resource pools.
 * Also, dependencies cannot be re-wired.
 * <p>
 * External event listeners can be registered and unregistered between evaluation windows
 * 
 * 
 * @author Steve Branda
 *
 */
public interface Simulation {
	
	public enum Status{
		RUNNING,
		PAUSED,
		DESTROYED;
	}
	
	public void registerGlobalConsumer(EventConsumer consumer);
	
	public void subscribeToEvents(EventConsumer consumer, Collection<String> eventSourceIds);
	
	public void subscribeToEvents(EventConsumer consumer, String eventSourceId);
	
	public long setWindowSize(long windowSize);
	
	public void start();
	
	public void start(long pauseAtTime);
	
	public void pause(boolean join) throws Throwable;
	
	public void join() throws Throwable;
	
	public void destroy();
	
	public Status getStatus();
	
	public long getTimeCompleted();
}
