/**
 * 
 */
package com.perelens.simulation.failure;

import java.util.HashMap;
import java.util.Map;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.core.AbstractEventEvaluator;
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Function;

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
public abstract class AbstractFailureFunction extends AbstractEventEvaluator implements Function{

	public static enum CONFIG_KEYS implements ConfigKey{
		FF_RESTORE_TIME;
	}
	
	protected static enum State{
		AVAILABLE,
		FAILED,
		RESTORING;
	}
	
	private State state = null;
	private int restoreTime = 0;
	private long returnToServiceTime = Long.MAX_VALUE;
	
	protected AbstractFailureFunction(String id) {
		super(id);
	}
	
	protected void checkConfigurable() {
		if (getTimeProcessed() > 0) {
			throw new IllegalStateException(FailureMsgs.reconfigurationNotAllowed());
		}
	}
	
	public void setRestoreTime(int restoreTime) {
		checkConfigurable();
		if (restoreTime < 0) {
			throw new IllegalArgumentException(FailureMsgs.badRestoreTime(restoreTime));
		}
		
		this.restoreTime = restoreTime;
	}
	
	protected long getReturnToServiceTime() {
		return returnToServiceTime;
	}
	
	protected void setReturnToServiceTime(long returnToService) {
		if (returnToService < getTimeProcessed()) {
			throw new IllegalArgumentException(FailureMsgs.timeMustAdvance(getTimeProcessed(),returnToService));
		}
		
		returnToServiceTime = returnToService;
	}
	
	protected void clearReturnToServiceTime() {
		returnToServiceTime = Long.MAX_VALUE;
	}
	
	protected boolean isReturnToServiceTimeSet() {
		return returnToServiceTime != Long.MAX_VALUE;
	}
	
	protected int getRestoreTime() {
		return restoreTime;
	}
	
	protected State getState() {
		return state;
	}
	
	protected void setState(State toSet) {
		Utils.checkNull(toSet);
		this.state = toSet;
	}

	protected void syncInternalState(AbstractFailureFunction toSync) {
		super.syncInternalState(toSync);
		toSync.restoreTime = this.restoreTime;
		toSync.returnToServiceTime = this.returnToServiceTime;
		toSync.state = this.state;
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		HashMap<ConfigKey,String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.FF_RESTORE_TIME, Integer.toString(getRestoreTime()));
		return toReturn;
	}
}
