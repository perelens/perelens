/**
 * 
 */
package com.perelens.simulation.statistics;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.HdrHistogram.Histogram;
import org.apache.commons.math3.distribution.BinomialDistribution;

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
public class DistributionSamples {

	private long[] samples = new long[1024];
	private int sIndex = 1;
	private boolean sorted = false;
	
	
	public DistributionSamples() {
		samples[0] = Long.MIN_VALUE;
	}
	
	public void addData(long[] data) {
		Utils.checkNull(data);
		int sizeNeeded = sIndex + data.length;
		if (sizeNeeded >= samples.length) {
			int newLen = Math.max(samples.length + samples.length, sizeNeeded);
			long temp[] = new long[newLen];
			System.arraycopy(samples, 0, temp, 0, sIndex);
			samples = temp;
		}
		System.arraycopy(data, 0, samples, sIndex, data.length);
		sIndex += data.length;
		sorted = false;
	}
	
	public void addData(long data) {
		samples = Utils.append(samples, data, sIndex);
		sIndex++;
		sorted = false;
	}
	
	private void sortSamples() {
		if (!sorted) {
			Arrays.sort(samples, 0, sIndex);
			sorted = true;
		}
	}
	
	public Long getHigh() {
		sortSamples();
		if (getN() == 0) {
			return null;
		}else {
			return samples[sIndex - 1];
		}
	}
	
	public Long getLow() {
		sortSamples();
		if (getN() == 0) {
			return null;
		}else {
			return samples[1];
		}
	}
	
	public double getMean() {
		if (sIndex == 1) {
			return 0;
		}else {
			long sum = 0;
			for (int i = 1; i < sIndex; i++) {
				sum += samples[i];
			}
			return ((double)sum)/getN();
		}
	}
	
	public int getN() {
		return sIndex - 1;
	}
	
	public double getPercentile(double percentile) {
		Utils.checkPercentage(percentile);
		sortSamples();
		
		double mostLikely = (sIndex) * percentile; //sIndex is already sample size + 1
		double lowerStatistic = Math.floor(mostLikely);
		double upperStatistic = Math.ceil(mostLikely);
		long lowerValue =  samples[(int)lowerStatistic]; //array is 0 indexed
		long upperValue = samples[(int) upperStatistic]; //array is 0 indexed
		
		if (lowerValue == upperValue) {
			return lowerValue;
		}
		
		double lowerWeight = 1 - (mostLikely - lowerStatistic);
		double upperWeight = 1 - (upperStatistic - mostLikely);
		
		return upperWeight * upperValue  + lowerWeight * lowerValue;
	}
	
	public ConfidenceInterval getConfidenceInterval(double percentile, double confidenceLevel) {
		Utils.checkPercentage(percentile);
		Utils.checkPercentage(confidenceLevel);
		
		if (sIndex == 1) {
			return new ConfidenceInterval(0,0,0);
		}
		
		sortSamples();
		
		BinomialDistribution bd = new BinomialDistribution(sIndex,percentile);
		double lowerPercentile = (1.0 - confidenceLevel)/2.0;
		double upperPercentile = 1 - lowerPercentile;
		
		int lowerBoundPosition = bd.inverseCumulativeProbability(lowerPercentile);
		int upperBoundPosition = bd.inverseCumulativeProbability(upperPercentile);
		
		if (upperBoundPosition >= sIndex) {
			upperBoundPosition = sIndex -1;
		}
		
		if (lowerBoundPosition == 0) {
			lowerBoundPosition = 1;
		}
		
		long lowerBoundValue = samples[lowerBoundPosition];
		long upperBoundValue = samples[upperBoundPosition];
		
		//Actual confidence level is exact so calculate
		confidenceLevel = bd.cumulativeProbability(upperBoundPosition) - bd.cumulativeProbability(lowerBoundPosition);
		
		return new ConfidenceInterval(lowerBoundValue,upperBoundValue,confidenceLevel);
	}
	
	public Histogram getHistogram() {
		long maxValue = Long.MIN_VALUE;
		if (sIndex == 1) {
			return new Histogram(1);
		}
		
		if (sorted) {
			maxValue = samples[sIndex - 1];
		}else {
			for (int i = 1; i < sIndex; i++) {
				long val = samples[i];
				if (val > maxValue) {
					maxValue = val;
				}
			}
		}
		
		Histogram toReturn = new Histogram(1,Math.max(maxValue, 2),Long.toString(maxValue).length());
		for (int i = 1; i < sIndex; i++) {
			toReturn.recordValue(samples[i]);
		}
		
		return toReturn;
	}
	
	public Collection<Long> getRawData(){
		
		return new AbstractCollection<>() {

			@Override
			public Iterator<Long> iterator() {
				return new Iterator<>() {
					int index = 1;
					int lastIndex = sIndex;
					boolean sortRec = sorted;
					
					@Override
					public boolean hasNext() {
						checkForModification();
						return index < sIndex;
					}
					@Override
					public Long next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						return samples[index++];
					}
					
					private void checkForModification(){
						if (sIndex != lastIndex || sortRec != sorted) {
							throw new ConcurrentModificationException();
						}
					}
				};
			}

			@Override
			public int size() {
				return getN();
			}
			
		};
	}
	
}
