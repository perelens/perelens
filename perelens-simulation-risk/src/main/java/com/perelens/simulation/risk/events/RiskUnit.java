/**
 * 
 */
package com.perelens.simulation.risk.events;

import com.perelens.engine.api.EventMagnitude.Unit;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * Enumeration member names are prefixed with 'RU_' because the name() method should return a unique value.
 * 
 * @author Steve Branda
 *
 */
public enum RiskUnit implements Unit {
	
	RU_QUANTITY,  //A measurable positive quantity.  Likely some kind of currency in the Risk domain, but not necessarily.
	
	RU_ORDINAL,   //A non-math object mapped to a decimal value.  For example 1.0, 2.0, 3.0 = low, medium, high.
	
	RU_VECTOR;    //A measurable positive or negative one dimensional quantity.  Likely some kind of quantified risk metric or cost savings.
}
