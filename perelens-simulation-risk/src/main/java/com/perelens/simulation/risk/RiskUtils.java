package com.perelens.simulation.risk;

import com.perelens.engine.core.CoreUtils;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.risk.events.RiskEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Utility code for Risk Models
 * 
 * 
 * @author Steve Branda
 *
 */
class RiskUtils extends Utils {

	public static final Distribution ZERO_DISTRIBUTION = CoreUtils.ZERO_DISTRIBUTION;
	public static final Distribution ONE_DISTRIBUTION = CoreUtils.ONE_DISTRIBUTION;
	
	
	static void checkRiskEventArg(RiskEvent toCheck) {
		checkNull(toCheck);
		
		if (toCheck.name().endsWith(RiskEvent.END)) {
			throw new IllegalArgumentException(RskMsgs.invalidEndEventUse(toCheck));
		}
	}
	
	static RiskEvent getEndEvent(RiskEvent startEvent) {
		RiskUtils.checkRiskEventArg(startEvent);
		
		String name = startEvent.name();
		return RiskEvent.valueOf(name + RiskEvent.END);
	}
}
