/**
 * 
 */
package com.perelens.statistics;

import java.util.function.UnaryOperator;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Class for computing confidence intervals using the LOG likelihood method.
 * 
 * References
 * https://online.stat.psu.edu/stat504/node/58/
 * https://thestatsgeek.com/2014/02/08/wald-vs-likelihood-ratio-test/
 * 
 * 
 * @author Steve Branda
 *
 */
public class LogLikeRatio {

	public static ConfidenceInterval binomialConfidenceInterval(long trials, long successes) {
		return binomialConfidenceInterval(trials,successes,0.95d);
	}
	
	public static ConfidenceInterval binomialConfidenceInterval(long trials, long successes, double confidenceLevel) {
		if (trials < successes) {
			throw new IllegalArgumentException("Trials must be greater than or equal to successes");
		}
		
		final double x = successes;
		final double n = trials;
		
		//Most likely estimate
		double mle = x/n; 
		
		//log likelihood function for the binomial
		UnaryOperator<Double> logl = (p) -> {return x * Math.log(p) + (n-x)*Math.log(1-p);};
		
		ChiSquaredDistribution chiSq = new ChiSquaredDistribution(1);
		double target = chiSq.inverseCumulativeProbability(confidenceLevel);
		//Horizontal line that determines confidence level
		double pctile = logl.apply(mle) - target/2;
		
		
		double left = 0;
		double right = 0;
		
		//find the left intercept
		for (double i = 0; i < 1; i+=0.001d) {
			double cur = logl.apply(i);
			if (cur >= pctile) {
				//The intercept was between the previous value of i and the current value of i
				double lasti = i - 0.001;
				
				if (i == 0.0) {
					left = 0;
				}else if (Math.abs(pctile - cur) <= Math.abs(pctile - logl.apply(lasti))) {
					left = i;
				}else {
					left = lasti;
				}
				//left intercept found so break loop
				break;
			}
		}
		
		//find the right intercept
		for (double i = 1.0d; i > 0; i-=0.001d) {
			double cur = logl.apply(i);
			if (cur >= pctile) {
				//The intercept was between the previous value of i and the current value of i
				double lasti = i + 0.001;
				
				if (i==1) {
					right = 1;
				}else if (Math.abs(pctile-cur) <= Math.abs(pctile - logl.apply(lasti))) {
					right = i;
				}else {
					right = lasti;
				}
				//right intercept found so break loop
				break;
			}
		}
		
		return new ConfidenceInterval(left,right,confidenceLevel);
	}
	
}
