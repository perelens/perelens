/**
 * 
 */
package com.perelens.simulation.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.perelens.engine.api.Event;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.TimeTranslator;

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
public class CoreTimeTranslator implements TimeTranslator {

	
	private Instant start;
	private ChronoUnit res;
	
	public CoreTimeTranslator(Instant startTime, ChronoUnit resolution) {
		Utils.checkNull(startTime);
		Utils.checkNull(resolution);
		start = startTime;
		res = resolution;
	}
	
	@Override
	public Instant getInstant(Event e) {
		return start.plus(e.getTime(),res);
	}

	@Override
	public TimeTranslator copy() {
		return this; //state-less and immutable
	}

	@Override
	public String getSetup() {
		StringBuilder toReturn = new StringBuilder();
		toReturn.append(start.toEpochMilli());
		toReturn.append(';');
		toReturn.append(res.name());
		return toReturn.toString();
	}

}
