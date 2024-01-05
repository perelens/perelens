/**
 * 
 */
package com.perelens.simulation.api;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * @author Steve Branda
 *
 */
public interface DistributionProvider {

	public enum Interval{
		OPEN,
		CLOSED
	}
	
	/**
	 * Return a Distribution that just returns a constant value
	 * 
	 * @param value
	 * @return
	 */
	public Distribution constant(double value);
	
	
	/**
	 * Returns a Distribution that behaves like a uniform distribution parameterized with the passed upper and lower bounds.
	 * 
	 * @param lower
	 * @param upper
	 * @param intervalType
	 * @return
	 */
	public Distribution uniform(double lower, double upper, Interval intervalType);
	
	
	/**
	 * Returns a Distribution that behaves like the Exponential distribution parameterized with the passed mean value
	 * 
	 * @param mean
	 * @return
	 */
	public Distribution exponential(double mean);
	
	/**
	 * Returns a Distribution that behaves like the Exponential distribution such that the desired percentile is located at
	 * the indicated value.
	 * This is useful for converting Annual Rate of Occurrence risks to an Exponential distribution.
	 * 
	 * @param percentile
	 * @param atValue
	 * @return
	 */
	public Distribution exponential(double percentile, double atValue);
	
	/**
	 * Returns a Distribution that behaves like the Lognormal distribution parameterized with the passed parameters
	 * 
	 * @param mean
	 * @param standardDeviation
	 * @return
	 */
	public Distribution lognormal(double mean, double standardDeviation);
	
	/**
	 * Returns a Distribution that behaves like the Lognormal distribution parameterized such that 90% of sampled values
	 * fall between the passed lower and upper bound.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public Distribution lognormal90pctCI(double lowerBound, double upperBound);
	
	
	/**
	 * Returns a Distribution equivalent to the that provided the setup argument.
	 * 
	 * @param setup
	 * @return
	 */
	public Distribution getDistribution(String setup);
	
}
