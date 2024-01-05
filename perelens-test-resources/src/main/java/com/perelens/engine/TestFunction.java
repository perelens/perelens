/**
 * 
 */
package com.perelens.engine;

import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.EvaluatorResources;
import com.perelens.engine.api.EventGenerator;
import com.perelens.simulation.api.Function;
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
public class TestFunction implements Function {

	private String id;
	private Map<ConfigKey,String> config;
	
	public TestFunction(String id, Map<ConfigKey, String> configuration) {
		this.id = id;
		this.config = new HashMap<>(configuration);
	}
	
	@Override
	public void consume(long timeWindow, EvaluatorResources resources) {
	}

	@Override
	public EventGenerator copy() {
		return new TestFunction(id,config);
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		return config;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void initiate(FunctionInfo info) {
	}
}
