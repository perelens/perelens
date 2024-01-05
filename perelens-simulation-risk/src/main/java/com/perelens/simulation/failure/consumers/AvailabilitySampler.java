/**
 * 
 */
package com.perelens.simulation.failure.consumers;

import com.perelens.engine.api.Event;
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
public class AvailabilitySampler extends AbstractFailureConsumer {

	private long sampleUptime = 0;
	private long sampleDowntime = 0;
	private boolean sampleStartRecorded = false;
	private boolean sampleEndRecorded = false;
	private double[] samples = new double[1024];
	private int sIndex = 0;
	
	private long sampleDuration;
	private long sampleStartTime;
	private long sampleEndTime;
	private long timeBetweenSamples;
	
	public AvailabilitySampler(String id, long firstSampleTime, long sampleDuration, long timeBetweenSamples) {
		super(id);
		Utils.checkArgNotNegative(firstSampleTime);
		Utils.checkArgStrictlyPositive(sampleDuration);
		Utils.checkArgStrictlyPositive(timeBetweenSamples);
		
		this.sampleDuration = sampleDuration;
		this.timeBetweenSamples = timeBetweenSamples;
		
		sampleStartTime = firstSampleTime;
		sampleEndTime = sampleStartTime + sampleDuration;
		
	}
	@Override
	protected void postInvoke() {
		super.postInvoke();
		
		long sampledTime = calculateSampleTime(getTimeWindow());
		if (isFailed()) {
			sampleDowntime += sampledTime;
		}else {
			sampleUptime += sampledTime;
		}
		
		setupNextSampleIfNecessary(getTimeWindow(),false);
	}
	
	private long calculateSampleTime(final long currentTime) {
		long toReturn = 0;
		if (getLastTime() < sampleStartTime && currentTime >= sampleStartTime && !sampleStartRecorded) {
			//The sample started between the last event or time window end and the current time
			toReturn = Math.min(currentTime, sampleEndTime) - sampleStartTime;
		}else if (getLastTime() >= sampleStartTime && currentTime >= sampleEndTime && !sampleEndRecorded) {
			//The sample ended between the last event or time window end and the current time
			toReturn = Math.min(currentTime, sampleEndTime) - getLastTime();
		}else if (getLastTime() >= sampleStartTime && currentTime < sampleEndTime) {
			//The time between the last event or time window end and the current time is completely within
			//the sample period
			toReturn = currentTime - getLastTime();
		}
		
		sampleStartRecorded = sampleStartRecorded || (currentTime >= sampleStartTime);
		sampleEndRecorded = sampleEndRecorded || (currentTime >= sampleEndTime);
		
		return toReturn;
	}
	
	private void setupNextSampleIfNecessary(final long currentTime, final boolean isEvent) {
		if (sampleStartRecorded && sampleEndRecorded) {
			//The sample ended between the last event or time window end and the current time
			
			//Record the last sample
			double availability = sampleUptime/(double)(sampleUptime + sampleDowntime);
			Utils.checkPercentage(availability);
			if (sIndex == samples.length) {
				double[] temp = new double[samples.length + samples.length];
				System.arraycopy(samples, 0, temp, 0, sIndex);
				samples = temp;
			}
			
			samples[sIndex] = availability;
			sIndex++;
			
			sampleUptime = 0;
			sampleDowntime = 0;
			sampleStartRecorded = false;
			sampleEndRecorded = false;
			
			//set the next sample interval
			sampleStartTime = sampleEndTime + timeBetweenSamples;
			sampleEndTime = sampleStartTime + sampleDuration;
			
			//It is possible the next sample interval starts before the current time
			//so capture any time
			long sampledTime = calculateSampleTime(currentTime);
			//Need to apply the sample time to uptime and downtime differently if the current time
			//is from an event vs the termination of a time window
			if (isEvent) {
				if (isFailed()) {
					sampleUptime += sampledTime;
				}else {
					sampleDowntime += sampledTime;
				}
			}else {
				if (isFailed()) {
					sampleDowntime += sampledTime;
				}else {
					sampleUptime += sampledTime;
				}
			}
			
			//Need to call recursively in case there are multiple sample periods in
			//The block of time being examined
			if (sampleStartRecorded && sampleEndRecorded) {
				setupNextSampleIfNecessary(currentTime, isEvent);
			}
		}
	}

	@Override
	protected void processChange(Event currentEvent) {
		long sampledTime = calculateSampleTime(currentEvent.getTime());
		if (isFailed()) {
			sampleUptime += sampledTime;
		}else {
			sampleDowntime += sampledTime;
		}
		
		setupNextSampleIfNecessary(currentEvent.getTime(),true);
	}
	
	public double[] getSamples() {
		double[] toReturn = new double[sIndex];
		System.arraycopy(samples, 0, toReturn, 0, sIndex);
		return toReturn;
	}

}
