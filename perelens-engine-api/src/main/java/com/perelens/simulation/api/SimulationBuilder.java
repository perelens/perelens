/**
 * 
 */
package com.perelens.simulation.api;

import java.util.Map;

import com.perelens.engine.api.ConfigKey;

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
public interface SimulationBuilder {

	/**
	 * Add a Function to the SimulationBuilder. The Function will be copied when createSimulation() is called.
	 * This means the resulting simulation will not affect the internal state of the SimulationBuilder
	 * and a subsequent call to createSimulation() will generate another identical simulation.
	 * 
	 * @param f
	 * @return
	 */
	public FunctionReference addFunction(Function f);
	
	/**
	 * Add a ResourcePool to the SimulationBuilder. The ResourcePool will be copied when createSimulation() is called.
	 * This means the resulting simulation will not affect the internal state of the SimulationBuilder
	 * and a subsequent call to createSimulation() will generate another identical simulation.
	 * 
	 * @param p
	 * @return
	 */
	public SimulationBuilder addResourcePool(ResourcePool p);
	
	/**
	 * Set the RandomProvider that will be used to create RandomGenerators for all Functions that need them.
	 * Unlike the simulation objects, the RandomProvider is NOT copied inside of the SimulationBuilder and may
	 * have its internal state changed by each call to createSimulation().
	 * This means the RandomProvider may need to be recreated if the goal is to have a subsequent call to
	 * createSimultion() produce an exact replica of the previous simulation.
	 * The default functionality would be to produce a simulation with the same configuration, but with different
	 * RandomGenerators, which is the most useful default behavior.
	 * 
	 * @param p
	 * @return
	 */
	public SimulationBuilder setRandomProvider(RandomProvider p);
	
	/**
	 * Set the TimeTranslator that will be used to convert Simulation time to real world time
	 * 
	 * @param t
	 * @return
	 */
	public SimulationBuilder setTimeTranslator(TimeTranslator t);
	
	
	/**
	 * Returns true if a resource pool with the given id exists.
	 * 
	 * @param poolId
	 * @return
	 */
	public boolean resourcePoolExists(String poolId);
	
	
	/**
	 * Gets a FunctionReference to the function with the passed id, if it exists.
	 * Otherwise returns null;
	 * 
	 * @param id
	 * @return
	 */
	public FunctionReference getFunction(String id);
	
	/**
	 * Return the configuration of the Simulation Object if it exists.
	 * Otherwise returns null;
	 * 
	 * @param id
	 * @return
	 */
	public Map<ConfigKey,String> getConfig(String id);
	
	/**
	 * Creates a runnable Simulation that reflects the configuration of this SimulationBuilder.
	 * This method will create a deep copy of all simulation objects so that this builder
	 * can be used to create multiple simulation runs for statistical analysis.
	 * 
	 * 
	 * @param parallelism - maximum number of threads the simulation engine should attempt to use
	 * @return
	 */
	public Simulation createSimulation(int parallelism);
	
	/**
	 * Creates a runnable Simulation and then destroys the internal state of this SimulationBuilder.
	 * This method will not create a deep copy of all simulation objects and therefore multiple
	 * simulations cannot be generated from this builder.
	 * 
	 * This is primarily useful for constructing extremely large simulations where there may not
	 * be enough memory to create a deep copy of every simulation object.
	 * 
	 * @param parallelism - maximum number of threads the simulation engine should attempt to use
	 * @return
	 */
	public Simulation createSimulationAndDestroy(int parallelism);
	
	/**
	 * Returns a cryptographically strong hash of the SimulationBuilder configuration.
	 * If two simulation builders with the same hash code create Simulations with the same initState, then those simulations should generate exactly the same sequence of events.
	 * 
	 * @return
	 */
	public String getHashCode();
	
}