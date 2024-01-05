/**
 * 
 */
package com.perelens.simulation.statistics;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import com.perelens.engine.utils.Utils;
import com.perelens.statistics.ConfidenceInterval;

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
public class SampledStatistic {

	private Mean mean;
	private StandardDeviation sampleStdDev;
	private double high = Double.MIN_VALUE;
	private double low = Double.MAX_VALUE;
	
	public SampledStatistic() {
		mean = new Mean();
		sampleStdDev = new StandardDeviation(true); //bias-corrected is true to get sample standard devaition
	}
	
	public void add(double observation) {
		mean.increment(observation);
		sampleStdDev.increment(observation);
		high = Math.max(observation, high);
		low = Math.min(observation, low);
	}
	
	public void add(double[] observations) {
		Utils.checkNull(observations);
		for(double cur : observations) {
			add(cur);
		}
	}
	
	public double getMean() {
		return mean.getResult();
	}
	
	public double getStandardDeviation() {
		return sampleStdDev.getResult();
	}
	
	public long getN() {
		return mean.getN();
	}
	
	public ConfidenceInterval getConfidenceInterval(double confidenceLevel) {
		
		TDistribution td = new TDistribution(getN() - 1);
		double cl = 1.0 - (1.0d - confidenceLevel)/2.0d;
		double t = td.inverseCumulativeProbability(cl);
		double stdDev = getStandardDeviation();
		double interval = t * stdDev/Math.sqrt(getN());
		
		return new ConfidenceInterval(getMean() - interval,getMean() + interval,confidenceLevel);
	}
	
	public Double getHigh() {
		if (mean.getN() == 0) {
			return null;
		}else {
			return high;
		}
	}
	
	public Double getLow() {
		if (mean.getN() == 0) {
			return null;
		}else {
			return low;
		}
	}
}
