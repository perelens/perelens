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
import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.FunctionInfo;
import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.failure.events.FailureSimulationEvent;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Compound function that is functional if minimum number (K) of its total dependencies (N) of are available.
 * 
 * @author Steve Branda
 *
 */
public class FunctionKofN extends AbstractFailureFunction{

	public static enum CONFIG_KEYS implements ConfigKey{
		KNFF_MIN_REQUIRED_DEPENDENCIES,
		KNFF_TOTAL_DEPENDENCIES;
	}
	
	private int k;
	private int n;
	
	private Event[] events = com.perelens.engine.utils.Utils.EMPTY_QUEUE;
	private int eIndex = 0;
	private boolean eCompress = false;
	
	private Event failureEvent = null;
	private int totalFailures = 0;
	
	private double failOverFaultPercentage = 0;
	private RandomGenerator rngFailOverFault = null;
	
	//Mean time to fail over functionality
	private int mtfo = 0;
	private RandomGenerator rngMTFO = null;
	private Distribution mtfoDistribution = null;
	
	public FunctionKofN(String id, int k, int n) {
		super(id);
		if (k <= 0 || n <= 0 || k > n) {
			throw new IllegalArgumentException(FailureMsgs.usageKofN());
		}
		
		this.k = k;
		this.n = n;
		setState(State.AVAILABLE);
	}
	
	public void setMeanTimeToFailOver(int mtfo) {
		if (mtfo <= 0) {
			throw new IllegalArgumentException(FailureMsgs.mustBeGreaterThanOrEqualToZero(mtfo));
		}
		this.mtfo = mtfo;
		mtfoDistribution = null;
	}
	
	public void setTimeToFailOver(Distribution mtfo) {
		Utils.checkNull(mtfo);
		mtfoDistribution = mtfo;
		this.mtfo = 0;
	}
	
	public void setFailOverFaultPercentage(double p) {
		Utils.checkPercentage(failOverFaultPercentage);
		this.failOverFaultPercentage = p;
	}
	
	@Override
	public void initiate(FunctionInfo info) {
		Set<String> deps = info.getDependencies();
		
		if (deps.size() < n) {
			throw new IllegalStateException(FailureMsgs.missingDependencies(deps.size(), n));
		}else if (deps.size() > n) {
			throw new IllegalStateException(FailureMsgs.extraDependencies(deps.size(),n));
		}
		
		if (failOverFaultPercentage > 0) {
			rngFailOverFault = info.getRandomGenerator();
		}
		
		if (mtfoDistribution != null) {
			rngMTFO = info.getRandomGenerator();
		}
	}

	
	protected int getK() {
		return k;
	}
	
	protected int getN() {
		return n;
	}
	
	protected int getTotalFailures() {
		return totalFailures;
	}
	
	@Override
	protected void process(Event curEvent) {
		//Common processing for all states
		if (curEvent != null) {
			if (this.getId() == "system a") {
				//System.out.println(curEvent);
			}
			//Only interested in FAILED or RETURN_TO_SERVICE events from dependencies
			if (curEvent.getType() != FailureSimulationEvent.FS_FAILED && curEvent.getType() != FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				return;
			}else {
				storeEvent(curEvent);
			}
			
			//Track total number of failures
			if (curEvent.getType() == FailureSimulationEvent.FS_FAILED) {
				totalFailures++;
				if (totalFailures > n) {
					throw new IllegalStateException(FailureMsgs.badState());
				}
			}else if (curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				totalFailures--;
			}
		}
		
		if (getState() == State.RESTORING) {
			if (totalFailures > n-k) {
				//The current event is a failure that interrupted restoration.
				//Return to the failed state but do not raise another failure event
				setStateFailed(getFailureEvent());
			}else if (getReturnToServiceTime() == getTimeProcessed()) {
				//Raise the RETURN_TO_SERVICE event
				//Get all the events that occurred since the failure event
				Event[] sf = getEventsSinceFailure(getReturnToServiceTime());
				Event re = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,this.getId(),getReturnToServiceTime(),getNextOrdinal(),sf);
				raiseEvent(re);
				
				//Move to the AVAILABLE state
				setStateAvailable(curEvent);
			}
		}
		
		//When in the AVAILABLE state, the internal state will only change in response to events
		if (getState() == State.AVAILABLE && curEvent != null) {
			if (curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE) {
				//In the AVAILABLE state, the function only needs to store FS_FAILED events from its dependencies.
				//When a dependency becomes available again due to an FS_RETURN_TO_SERVICE event, that event and the original FS_FAILED event can be purged.
				clearFailure(curEvent);
			}
			
			//When n-k + 1 total failures accumulates, the function moves from AVAILABLE to FAILED and raises an FS_FAILED event that contains the
			//FS_FAILURE events for all the currently failed dependencies.
			if (totalFailures > n-k) {
				Event[] depFails = getFailureEvents();
				Event fe = new FailSimEvent(FailureSimulationEvent.FS_FAILED, this.getId(), curEvent.getTime(), getNextOrdinal(),depFails);
				raiseEvent(fe);
				setStateFailed(curEvent);
			}else if(curEvent.getType() == FailureSimulationEvent.FS_FAILED) {
				//If an FS_FAILURE event does not result in n-k + 1 total failures, but the function is configured with a possibility of failover fault,
				//meaning the function should stay in the AVAILABLE state, sample a binomial distribution to simulate if a failover fault occurred.
				//If a failover fault occurs the function moves from AVAILABLE to FAILED for the duration of system restore time.
				
				if (failOverFaultPercentage > 0.0) {
					if(FailureUtils.processFails(failOverFaultPercentage,rngFailOverFault.nextDouble())) {
						//Fail over fault raise the failure and move through FAILED to RESTORING immediately
						Event failFaultEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILOVER_FAULT,this.getId(),curEvent.getTime(),getNextOrdinal(),curEvent);
						Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED,this.getId(),failFaultEvent.getTime(),getNextOrdinal(),failFaultEvent);
						raiseEvent(failEvent);
						setStateFailed(curEvent);
						setStateRestoring(failFaultEvent);
					}
				}
				
				if (getState() == State.AVAILABLE && shouldProcessMTFO(curEvent)) {
					//A failover fault did not occur but the function does have a Mean Time To Fail Over configured.
					//By definition the function is considered FAILED during this fail over time
					long mtfoToApply = mtfo;
					if (mtfoDistribution != null) {
						//Round up
						mtfoToApply = (long)Math.ceil(mtfoDistribution.sample(rngMTFO.nextDouble()));
					}
					
					//In theory the distribution could result in no MTFO, so check to make sure
					if (mtfoToApply > 0) {
						Event mtfoEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILING_OVER, this.getId(),curEvent.getTime(),getNextOrdinal(),curEvent);
						Event failEvent = new FailSimEvent(FailureSimulationEvent.FS_FAILED, this.getId(),curEvent.getTime(),getNextOrdinal(),mtfoEvent);
						raiseEvent(failEvent);
						setStateFailed(curEvent);
						setStateFailingOver(mtfoEvent,mtfoToApply);
					}
				}
			}
		}
		
		
		//In the FAILED state, the internal state of the function will only change in response to events
		if (getState() == State.FAILED && curEvent != null) {
			if (totalFailures <= n-k) {
				//When enough FS_RETURN_TO_SERVICE events are received to indicate that k dependencies are available move to RESTORING state
				setStateRestoring(curEvent);
			}
		}
		
		if (getId().equals("main cluster")) {
			if (totalFailures > 0) {
				//System.out.println("Total Failures = " + totalFailures + " . State = " + this.getState());
			}
		}
		
	}
	
	protected boolean shouldProcessMTFO(Event ev) {
		return mtfo > 0 || mtfoDistribution != null;
	}

	@Override
	public EventGenerator copy() {
		FunctionKofN toReturn = new FunctionKofN(getId(),k,n);
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(FunctionKofN toSync) {
		super.syncInternalState(toSync);
		toSync.events = Arrays.copyOf(this.events, this.events.length);
		toSync.eIndex = this.eIndex;
		toSync.eCompress = this.eCompress;
		toSync.failureEvent = this.failureEvent;
		toSync.rngFailOverFault = this.rngFailOverFault == null?null:this.rngFailOverFault.copy();
		toSync.failOverFaultPercentage = this.failOverFaultPercentage;
		toSync.totalFailures = this.totalFailures;

		toSync.mtfo = this.mtfo;
		toSync.rngMTFO = this.rngMTFO == null?null:this.rngMTFO.copy();
		toSync.mtfoDistribution = this.mtfoDistribution == null?null:this.mtfoDistribution.copy();
	}

	protected Event getFailureEvent() {
		return failureEvent;
	}
	
	protected Event[] getEventsSinceFailure(long timeLimit) {
		if (failureEvent == null) throw new IllegalStateException(FailureMsgs.badState());
		compressEvents();
		int start = -1;
		int end = eIndex;
		for (int i = 0; i < eIndex; i++) {
			if (events[i] == failureEvent) {
				start = i+1;
			}
			if (start >= 0 && events[i].getTime() > timeLimit) {
				end = i;
				break;
			}
		}
		
		if (start == -1) {
			throw new IllegalStateException(FailureMsgs.badState());
		}else {
			return Arrays.copyOfRange(events, start, end);
		}	
	}

	protected Event[] getFailureEvents() {
		Event[] toReturn = new Event[totalFailures];
		int idx = 0;
		for (int i = 0; i < eIndex; i++) {
			Event cur = events[i];
			if (cur != null && cur.getType() == FailureSimulationEvent.FS_FAILED) {
				toReturn[idx] = cur;
				idx++;
			}
		}
		
		if (idx != totalFailures) {
			throw new IllegalStateException(FailureMsgs.badTotalFailureState(totalFailures, idx));
		}
		return toReturn;
	}
	
	protected void clearFailure(Event repairEvent) {
		String repairedDep = repairEvent.getProducerId();
		for (int i = 0; i < eIndex; i++) {
			Event cur = events[i];
			if (cur != null && cur.getType() == FailureSimulationEvent.FS_FAILED && cur.getProducerId().equals(repairedDep)) {
				events[i] = null; //Clear the original failure
				if (events[eIndex - 1] == repairEvent) {
					events[--eIndex] = null; //Clear the repair
				}else {
					throw new IllegalStateException(FailureMsgs.badState());
				}
				eCompress = true;
				return;
			}
		}
		
		throw new IllegalStateException(FailureMsgs.unableToClearFailureEvent(repairEvent));
	}
	
	protected void setStateAvailable(Event repairEvent) {
		setState(State.AVAILABLE);
		clearReturnToServiceTime();
		failureEvent = null;
		
		//purge all events except for active FAILED dependencies and the repairEvent and its associated failure
		//The repairEvent and its associated FAILED event will be purged by AVAILABLE state processing
		for (int i = eIndex - 1; i >=0; i--) {
			Event curEvent = events[i];
			if (curEvent != null && curEvent.getType() == FailureSimulationEvent.FS_RETURN_TO_SERVICE && curEvent != repairEvent) {
				boolean cleared = false;
				String repairedDep = curEvent.getProducerId();
				for (int j = i - 1; j >= 0; j--) {
					Event cur = events[j];
					if (cur != null && cur.getType() == FailureSimulationEvent.FS_FAILED && cur.getProducerId().equals(repairedDep)) {
						events[j] = null; //Clear the original failure
						events[i] = null; //Clear the repair event
						cleared = true;
						break;
					}
				}
				
				if (!cleared) {
					throw new IllegalStateException(FailureMsgs.badState());
				}
			}
		}
		
		eCompress = true;
		compressEvents();
	}
	
	protected void setStateFailed(Event eventThatCausedFailure) {
		if (eventThatCausedFailure == null) {
			throw new IllegalArgumentException(FailureMsgs.argNotNull());
		}
		if (eventThatCausedFailure.getType() != FailureSimulationEvent.FS_FAILED) {
			throw new IllegalArgumentException(FailureMsgs.wrongEventType(FailureSimulationEvent.FS_FAILED,eventThatCausedFailure.getType()));
		}
		if (eventThatCausedFailure.getProducerId().equals(this.getId())) {
			throw new IllegalStateException(FailureMsgs.badState());
		}
		
		failureEvent = eventThatCausedFailure;
		clearReturnToServiceTime();
		setState(State.FAILED);
	}
	
	protected void setStateRestoring(Event fe) {
		Utils.checkNull(fe);
		if (fe.getType() != FailureSimulationEvent.FS_RETURN_TO_SERVICE && fe.getType() != FailureSimulationEvent.FS_FAILOVER_FAULT) {
			throw new IllegalArgumentException(FailureMsgs.wrongEventType(FailureSimulationEvent.FS_RETURN_TO_SERVICE, fe.getType()));
		}
		setReturnToServiceTime(fe.getTime() + getRestoreTime());
		this.registerCallbackTime(getReturnToServiceTime());
		setState(State.RESTORING);
	}
	
	//Almost the same as setStateRestoring but no system restore time
	protected void setStateFailingOver(Event fe, long mtfoVal) {
		Utils.checkNull(fe);
		if (fe.getType() != FailureSimulationEvent.FS_FAILING_OVER) {
			throw new IllegalArgumentException(FailureMsgs.wrongEventType(FailureSimulationEvent.FS_FAILING_OVER, fe.getType()));
		}
		setReturnToServiceTime(fe.getTime() + mtfoVal);
		this.registerCallbackTime(getReturnToServiceTime());
		setState(State.RESTORING);
	}
	
	protected void storeEvent(Event e) {
		compressEvents();
		events = com.perelens.engine.utils.Utils.append(events, e, eIndex);
		eIndex++;
	}
	
	protected void compressEvents() {
		if (eCompress) {
			eIndex = com.perelens.engine.utils.Utils.compress(events, eIndex);
			eCompress = false;
		}
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		HashMap<ConfigKey,String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.KNFF_MIN_REQUIRED_DEPENDENCIES, Integer.toString(k));
		toReturn.put(CONFIG_KEYS.KNFF_TOTAL_DEPENDENCIES, Integer.toString(n));
		return toReturn;
	}
}
