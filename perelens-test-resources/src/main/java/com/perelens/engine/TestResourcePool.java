/**
 * 
 */
package com.perelens.engine;

import java.util.Collections;
import java.util.Map;

import com.perelens.engine.api.AbstractEventConsumer;
import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.ConsumerResources;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventGenerator;
import com.perelens.engine.api.ResponderResources;
import com.perelens.simulation.api.BasicInfo;
import com.perelens.simulation.api.ResourcePool;

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
public class TestResourcePool extends AbstractEventConsumer implements ResourcePool {

	public TestResourcePool(String id) {
		super(id);
	}

	@Override
	public void consume(long timeWindow, ResponderResources resources) {
	}

	@Override
	public EventGenerator copy() {
		return new TestResourcePool(getId());
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		return Collections.emptyMap();
	}

	@Override
	public void consume(long timeWindow, ConsumerResources events) {
	}

	@Override
	public void initiate(BasicInfo info) {
	}

	@Override
	protected void processEvent(Event currentEvent) {
		
	}

}
