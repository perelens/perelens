/**
 * 
 */
package com.perelens.engine.core;

import com.perelens.engine.api.EvaluatorResources;
import com.perelens.engine.api.Event;

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
public abstract class AbstractEventEvaluator extends AbstractEventGenerator<EvaluatorResources> {

	protected AbstractEventEvaluator(String id) {
		super(id);
	}

	protected void raiseEvent(Event toRaise) {
		super.raiseEvent(toRaise);
		getResources().raiseEvent(toRaise);
	}
}
