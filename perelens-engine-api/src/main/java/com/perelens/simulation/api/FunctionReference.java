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
public interface FunctionReference{

	/**
	 * Register the passed function as a dependency of this function.
	 * The simulation engine will make sure the dependency has been executed for the given time window before this function.
	 * 
	 * @param dependency
	 * @return
	 * @throws IllegalArgumentException
	 */
	public FunctionReference addDependency(FunctionReference dependency);
	
	/**
	 * Register the function with the passed unique identifier as a dependency of this function.
	 * The simulation engine will make sure the dependency has been executed for the given time window before this function.
	 * 
	 * @param functionId
	 * @return
	 */
	public FunctionReference addDependency(String functionId);
	
	/**
	 * Remove the passed function as a dependency of this function.
	 * 
	 * @param dependency
	 * @return
	 * @throws IllegalArgumentException
	 */
	public FunctionReference removeDependency(FunctionReference dependency);
	
	
	/**
	 * Remove the function with the passed unique identifier as a dependency of this function
	 * 
	 * @param functionId
	 * @return
	 */
	public FunctionReference removeDependency(String functionId);
	
	/**
	 * @param poolId
	 * @return
	 */
	public FunctionReference addResourcePool(String poolId);
	
	/**
	 * @param poolId
	 * @return
	 */
	public FunctionReference removeResourcePool(String poolId);
	
	/**
	 * Returns all the dependencies of this function
	 * 
	 * @return
	 */
	public Set<FunctionReference> getDependencies();
	
	
	/**
	 * Returns the ids of all the ResourcePools used by this function
	 * 
	 * @return
	 */
	public Set<String> getResourcePools();
	
	/**
	 * Returns the unique id of the function
	 * 
	 * @return
	 */
	public String getId();
	
}
