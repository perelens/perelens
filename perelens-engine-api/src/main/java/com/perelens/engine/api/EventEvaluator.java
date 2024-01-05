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
 * The EventEvaluator interface is the primary interface for simulation participants.
 * When the simulation is configured and constructed, each EventEvaluator instance must report a unique id.
 * In order for a Simulation to be repeatable, these id's to be the same across runs of the simulation.
 * 
 * The consume method for EventEvaluators will only be invoked under the following conditions:
 * 
 * 1. All of the event generators the instance is subscribed to have completed for the current execution window.
 * 2. If it has raised an event that requires a response, it will be invoked when the response is provided.
 * 
 * @author Steve Branda
 *
 */
public interface EventEvaluator extends EventGenerator{
	
	public void consume(long timeWindow, EvaluatorResources resources);
	
}
