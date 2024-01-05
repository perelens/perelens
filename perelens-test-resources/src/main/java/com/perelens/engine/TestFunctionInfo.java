/**
 * 
 */
package com.perelens.engine;

import java.util.Collections;
import java.util.Set;

import com.perelens.simulation.api.FunctionInfo;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * @author Steve Branda
 *
 */
public abstract class TestFunctionInfo implements FunctionInfo {

	
	
	@Override
	public Set<String> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getResourcePools() {
		return Collections.emptySet();
	}

}
