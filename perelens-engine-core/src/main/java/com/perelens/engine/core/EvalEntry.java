/**
 * 
 */
package com.perelens.engine.core;

import java.util.Arrays;

import com.perelens.engine.api.EventEvaluator;
import com.perelens.engine.utils.Utils;

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
public class EvalEntry extends RespEntry {

	//EventEvaluator functionality
	private SubEntry[] subscribers = CoreUtils.NO_ENTRIES;		//The EventSubscribers that should receive events dispatched this EventEvaluator
	private int subIndex = 0;
	String lastEvents = Utils.EMPTY_STRING;
	
	EvalEntry(EventEvaluator object, CoreEngine engine) {
		super(object, engine);
	}
	
	EventEvaluator getObject() {
		return (EventEvaluator) super.getObject();
	}
	
	void addSubscriber(SubEntry toAdd) {
		subscribers = com.perelens.engine.utils.Utils.append(subscribers,toAdd, subIndex);
		subIndex++;
	}
	
	SubEntry[] getSubscribers() {
		if (subIndex < subscribers.length) {
			//Truncate the array so that it is iterable and also immutable since any change will extend and replace it
			subscribers = Arrays.copyOf(subscribers,subIndex);
		}
		return subscribers;
	}
	
	int getSubscriberCount() {
		return subIndex;
	}
	
	@Override
	boolean canStartEval() {
		return getDependencyCount() == 0;
	}
	
	@Override
	boolean isDetached() {
		return getDependencyCount() == 0 && getSubscriberCount() == 0;
	}
	
	//Evaluation Logic Methods
	@Override
	Runnable getEvaluator(CoreEngine engine, long timeOffset, long targetOffset) {
		return new EventEvaluatorLogic(this,timeOffset,targetOffset,engine);
	}

	@Override
	void registerAsActive() {
		//EventEvaluators should not register as active
	}
	
	
}
