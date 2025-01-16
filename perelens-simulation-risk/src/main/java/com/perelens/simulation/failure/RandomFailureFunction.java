/**
 * 
 */
package com.perelens.simulation.failure;

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
import com.perelens.simulation.core.ResPoolEvent;
import com.perelens.simulation.events.ResourcePoolEvent;
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
 *
 */
public class RandomFailureFunction extends AbstractFailureFunction {
	
	public static enum CONFIG_KEYS implements ConfigKey{
		RFF_FAILURE_ARRIVAL_DIST,
		RFF_REPAIR_TIME_DIST,
		RFF_RESOURCE_POOL;
	}
	
	private Distribution failureDistribution;
	private Distribution repairDistribution;
	private RandomGenerator failureGen;
	private RandomGenerator repairGen;
	
	private long nextFailureTime = -1;
	
	//These fields support time optimized resource pools
	private long nextReturnToServiceInterval = -1;
	private long toBeginRestore = -1;
	
	private String resourcePool = null;
	private Event repairResource = null;
	
	public RandomFailureFunction(String id, Distribution failureArrivalTimeDistribution, Distribution repairTimeDistribution) {
		super(id);
		Utils.checkNull(failureArrivalTimeDistribution);
		Utils.checkNull(repairTimeDistribution);
		failureDistribution = failureArrivalTimeDistribution;
		repairDistribution = repairTimeDistribution;
	}
	
	@Override
	public void initiate(FunctionInfo info) {
		failureGen = info.getRandomGenerator();
		repairGen = info.getRandomGenerator();
		
		Set<String> pools = info.getResourcePools();
		if (pools.size() > 0) {
			if (pools.size() > 1) {
				//Only support one resource pool right now
				throw new IllegalStateException(FailureMsgs.badState());
			}
			resourcePool = pools.iterator().next();
		}
	}

	@Override
	protected void preProcess() {
		if (getState() == null) {
			//Only happens the first time the function is invoked during the simulation
			//Doing this here instead of the constructor allows the random distribution sampling to be done in parallel across all the functions
			setStateAvailable();
		}else if (getState() == State.FAILED) {
			if (!this.isReturnToServiceTimeSet()) {
				if (getRepairResource() == null) {
					//Must have been deferred by the resource pool during the last window so send a RENEW request
					var renew = new ResPoolEvent(getId(),ResourcePoolEvent.RP_RENEW,getWindowStart() + 1, getNextOrdinal(),ResourcePoolEvent.REQUEST_RESPONSE_TYPES);
					raiseEvent(renew);
					this.waitForResponse();
				}else {
					//Time optimized resource pool and just waiting to start repair so null op.
				}
			}else {
				throw new IllegalStateException(FailureMsgs.badState());
			}
		}
	}

	@Override
	protected void postProcess() {
		if (getRepairResource() != null) {
			if (getRepairResource().getTimeOptimization() == -1) {
				//Not Time optimized
				long resTime = getRepairResource().getTime();

				if (resTime > getWindowStart()) {
					//repair resource was received in this time window, but will be kept at least until the next window
					//Need to raise a DEFERRED event so the Resource Pool can complete this time window
					Event defer = new ResPoolEvent(getId(),ResourcePoolEvent.RP_DEFER,getTimeProcessed(), getNextOrdinal());
					raiseResponse(defer, getRepairResource());
				}
				
				//Sanity check
				if (getState() != State.RESTORING) {
					throw new IllegalStateException(FailureMsgs.badState());
				}
			}
		}
	}

	@Override
	protected void process(Event curEvent) {
		if (getState() == State.AVAILABLE) {
			if (getNextFailureTime() == getTimeProcessed()) {
				//Failure time has arrived so raise failure event
				Event toRaise = new FailSimEvent(FailureSimulationEvent.FS_FAILED,getId(),getTimeProcessed(),getNextOrdinal());
				raiseEvent(toRaise);
				
				setStateFailed();
				
				//request the repair resource if necessary
				if (getResourcePool() == null) {
					//No resource pool configured so move to RESTORING immediately
					setStateRestoring();
				}else {
					var resRequest = new ResPoolEvent(
							getId(),ResourcePoolEvent.RP_REQUEST,getTimeProcessed(),getNextOrdinal(),ResourcePoolEvent.REQUEST_RESPONSE_TYPES);
					resRequest.setTimeOptimization(getNextReturnToServiceInterval());
					raiseEvent(resRequest);
					super.waitForResponse();
				}
			}
		}
		
		
		if (getState() == State.FAILED) {
			if (!this.isReturnToServiceTimeSet()) {
				//Must be waiting for resource from the pool
				if (curEvent != null) {
					if (curEvent.getType() == ResourcePoolEvent.RP_GRANT) {
						long tOpt = curEvent.getTimeOptimization();
						if (tOpt >= curEvent.getTime()) {
							//Time optimized resource pool
							toBeginRestore = tOpt;
							this.registerCallbackTime(getToBeginRestore());
						}else {
							//Not time optimized resource pool
							setStateRestoring();
						}
						setRepairResource(curEvent);
					}
				}else if(getTimeProcessed() == getToBeginRestore()){
					setStateRestoring();
				}
			}else {
				throw new IllegalStateException(FailureMsgs.badState());
			}
		}
		
		
		if (getState() == State.RESTORING) {
			if (getReturnToServiceTime() == getTimeProcessed()) {
				if (getResourcePool() != null && getRepairResource().getTimeOptimization() == -1) {
					//Not Time optimized
					//Return the repair resource
					Event retRes = new ResPoolEvent(getId(),ResourcePoolEvent.RP_RETURN,getReturnToServiceTime(), getNextOrdinal());
					if (getRepairResource().getTime() > getWindowStart()) {
						//If the resource is received and the repair completes in the same time window then return through response
						raiseResponse(retRes, getRepairResource());
					}else {
						//If the resource was received during previous time window then return as an event
						raiseEvent(retRes);
					}
				}
				//Generate return to service event and move back to available state
				Event toRaise = new FailSimEvent(FailureSimulationEvent.FS_RETURN_TO_SERVICE,getId(),getReturnToServiceTime(),getNextOrdinal());
				raiseEvent(toRaise);

				setStateAvailable();
			}
		}
	}
	
	protected void setStateAvailable() {
		clearRepairResource();
		clearReturnToServiceTime();
		
		//generate the next failure time
		double uniformRandom = failureGen.nextDouble();
		double nextDuration = failureDistribution.sample(uniformRandom);
		//Cast to long which rounds down to to a complete unit.
		//This is a more conservative way to process failure arrival than rounding to the nearest complete unit, which would
		//extend failure arrival in 50% of cases.
		long nd = Math.max((long)nextDuration,1); //need to advance time at least one unit
		setNextFailureTime(getTimeProcessed() + nd);
		setState(State.AVAILABLE);
		this.registerCallbackTime(getNextFailureTime());
	}

	private void setStateFailed() {
		setState(State.FAILED);
		
		//Figure out the randomized repair time at the time of failure to support time optimized resource pools
		double uniformRandom = repairGen.nextDouble();
		double repairTime = repairDistribution.sample(uniformRandom);
		nextReturnToServiceInterval = ((long)Math.ceil(repairTime)) + getRestoreTime();
	}

	protected void setStateRestoring() {
		long repairStartTime = getTimeProcessed();
		
		this.setReturnToServiceTime(repairStartTime + nextReturnToServiceInterval);
		this.registerCallbackTime(getReturnToServiceTime());
		
		setState(State.RESTORING);
	}
	
	@Override
	public EventGenerator copy() {
		//the distributions will be copied in the syncInternalState call
		RandomFailureFunction toReturn = new RandomFailureFunction(getId(),failureDistribution,repairDistribution);
		syncInternalState(toReturn);
		return toReturn;
	}
	
	protected void syncInternalState(RandomFailureFunction toSync) {
		super.syncInternalState(toSync);
		toSync.failureGen = this.failureGen == null?null:this.failureGen.copy();
		toSync.repairGen = this.repairGen == null?null:this.repairGen.copy();
		toSync.nextFailureTime = this.nextFailureTime;
		toSync.resourcePool = this.resourcePool;
		toSync.repairResource = this.repairResource;
	}
	
	protected long getNextReturnToServiceInterval() {
		return nextReturnToServiceInterval;
	}
	
	protected long getToBeginRestore() {
		return toBeginRestore;
	}
	
	protected long getNextFailureTime() {
		return nextFailureTime;
	}
	
	protected void setNextFailureTime(long nextFailure) {
		if (nextFailure <= nextFailureTime) {
			throw new IllegalArgumentException(FailureMsgs.timeMustAdvance(nextFailureTime,nextFailure));
		}
		this.nextFailureTime = nextFailure;
	}
	
	protected Event getRepairResource() {
		return repairResource;
	}
	
	protected void setRepairResource(Event e) {
		Utils.checkNull(e);
		repairResource = e;
	}
	
	protected void clearRepairResource() {
		repairResource = null;
	}
	
	protected String getResourcePool() {
		return resourcePool;
	}

	@Override
	public Map<ConfigKey, String> getConfiguration() {
		Map<ConfigKey, String> toReturn = new HashMap<>(super.getConfiguration());
		toReturn.put(CONFIG_KEYS.RFF_FAILURE_ARRIVAL_DIST, failureDistribution.getSetup());
		toReturn.put(CONFIG_KEYS.RFF_REPAIR_TIME_DIST, repairDistribution.getSetup());
		toReturn.put(CONFIG_KEYS.RFF_RESOURCE_POOL, resourcePool==null?Utils.EMPTY_STRING:resourcePool);
		return toReturn;
	}

}
