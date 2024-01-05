/**
 * 
 */
package com.perelens.simulation.failure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.perelens.engine.api.ConfigKey;
import com.perelens.engine.api.Event;
import com.perelens.engine.api.EventGenerator;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.failure.events.FailureSimulationEvent;

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
public class ActivePassiveKofN extends FunctionKofN {

	public static enum CONFIG_KEYS implements ConfigKey{
		APKN_ACTIVE_NODES;
	}
	
	private static final String[] EMPTY_DEPS = new String[0];
	
	private int activeNodes;
	private String[] dependencies = EMPTY_DEPS;
	private boolean[] activeFlags = null;
	
	public ActivePassiveKofN(String id, int k, int n) {
		super(id, k, n);
		activeNodes = n;
	}
	
	public void setActiveNodes(int activeNodes) {
		if (activeNodes < getK() || activeNodes > getN()) {
			throw new IllegalArgumentException(FailureMsgs.invalidActiveNodes(activeNodes,getK(),getN()));
		}
		this.activeNodes = activeNodes;
	}

	@Override
	public void initiate(FunctionInfo info) {
		super.initiate(info);
		
		if (activeNodes < getN()) {
			//Minimal setup since initiate is single threaded
			Set<String> deps = info.getDependencies();
			dependencies = deps.toArray(EMPTY_DEPS);
		}
	}
	
	
	
	@Override
	protected void preProcess() {
		if (getTimeProcessed() == 0) {
			if (dependencies != EMPTY_DEPS && activeFlags == null) {
				//Finish setup here since it can be multi-threaded
				Arrays.sort(dependencies);
				activeFlags = new boolean[dependencies.length + dependencies.length];
				int marked = 0;
				for (int i=0; i < activeFlags.length; i+=2) {
					if (marked < activeNodes) {
						activeFlags[i] = true;
						marked++;
					}else {
						activeFlags[i] = false;
					}
					markAvailable(i);
				}
			}
		}
		
		super.preProcess();
	}

	private int getIndex(String id) {
		int index = Arrays.binarySearch(dependencies, id);
		return index + index;
	}
	
	private boolean isActive(int index) {
		return activeFlags[index];
	}
	
	private boolean isAvailable(int index) {
		return activeFlags[index + 1];
	}
	
	private void markAvailable(int index) {
		if (!activeFlags[index + 1]) {
			activeFlags[index + 1] = true;
		}else {
			throw new IllegalStateException(FailureMsgs.badState());
		}
	}
	
	private void markFailed(int index) {
		if (activeFlags[index + 1]) {
			activeFlags[index + 1] = false;
		}else {
			throw new IllegalStateException(FailureMsgs.badState());
		}
	}
	
	private void markActive(int index) {
		if (!activeFlags[index]) {
			activeFlags[index] = true;
		}else {
			throw new IllegalStateException(FailureMsgs.badState());
		}
	}
	
	private void markInactive(int index) {
		if (activeFlags[index]) {
			activeFlags[index] = false;
		}else {
			throw new IllegalStateException(FailureMsgs.badState());
		}
	}
	
	private void deactivate(int index) {
		int failoverTarget = -1;
		for (int i = 0; i < activeFlags.length; i+=2) {
			if (i != index && isAvailable(i) && !isActive(i)) {
				failoverTarget = i;
				break;
			}
		}
		
		markInactive(index);
		if (failoverTarget != -1) {
			markActive(failoverTarget);
		}
	}

	@Override
	protected boolean shouldProcessMTFO(Event ev) {
		boolean toReturn = super.shouldProcessMTFO(ev);
		
		if (toReturn && activeNodes < getN()) {
			//MTFO is configured but it should only be applied if one of the active nodes failed
			String id = ev.getProducerId();
			int index = getIndex(id);
			if (isActive(index)) {
				deactivate(index);
				return true;
			}else {
				return false;
			}
		}
		
		return toReturn;
	}

	@Override
	protected void process(Event curEvent) {
		State startState = getState();
		int index = -1;
		if (activeNodes < getN() && curEvent != null) {
			if (curEvent.getType() == FailureSimulationEvent.FS_FAILED) {
				String id = curEvent.getProducerId();
				index = getIndex(id);
				markFailed(index);
			}else if (curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				String id = curEvent.getProducerId();
				index = getIndex(id);
				markAvailable(index);
				if (startState == State.FAILED) {
					//Every repaired dependency should be marked as active
					//Until the function moves to RESTORING state
					markActive(index);
				}
			}
		}
		
		super.process(curEvent);
		
		if (getState() == State.FAILED && startState != State.FAILED && index != -1) {
			//Moved into full failure so we need to mark the last failed dependency as inactive
			markInactive(index);
		}
	}

	protected void syncInternalState(ActivePassiveKofN toSync) {
		super.syncInternalState(toSync);
		if (this.activeFlags != null) {
			toSync.activeFlags = Arrays.copyOf(this.activeFlags, this.activeFlags.length);
		}
		toSync.dependencies = this.dependencies;
		toSync.activeNodes = this.activeNodes;
	}

	@Override
	public EventGenerator copy() {
		ActivePassiveKofN toReturn = new ActivePassiveKofN(getId(),getK(),getN());
		syncInternalState(toReturn);
		return toReturn;
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		HashMap<ConfigKey, String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.APKN_ACTIVE_NODES, Integer.toString(activeNodes));
		return toReturn;
	}
}
