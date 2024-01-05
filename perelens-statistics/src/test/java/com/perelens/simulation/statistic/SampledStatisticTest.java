/**
 * 
 */
package com.perelens.simulation.statistic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.perelens.simulation.statistics.SampledStatistic;
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
class SampledStatisticTest {

	/**
	 * Sample T Distribution Confidence Interval Problem From
	 * 
	 * https://www.statisticshowto.datasciencecentral.com/probability-and-statistics/confidence-interval/
	 * 
	 */
	@Test
	void testScenario1() {
		double[] data = new double[] {45.0,55.0,67.0,45.0,68.0,79.0,98.0,87.0,84.0,82.0};
		
		SampledStatistic m = new SampledStatistic();
		m.add(data);
		
		assertEquals(18.172, m.getStandardDeviation(), 0.001);
		assertEquals(71.0, m.getMean(), 0.1);
		
		ConfidenceInterval ci = m.getConfidenceInterval(0.98);
		assertEquals(54.77925,ci.getLowerBound(), 0.01);
		assertEquals(87.22075, ci.getUpperBound(), 0.01);
	}
	
	
	
	/**
	 * First sample T distribution Confidence Interval problem from
	 * 
	 * http://onlinestatbook.com/2/estimation/mean.html
	 * 
	 */
	@Test
	void testScenario2() {
		double[] data = new double[] {2,3,5,6,9};
		
		SampledStatistic m = new SampledStatistic();
		m.add(data);
		
		assertEquals(7.5, Math.pow(m.getStandardDeviation(),2), 0.001);
		assertEquals(5, m.getMean(), 0.1);
		
		ConfidenceInterval ci = m.getConfidenceInterval(0.95);
		assertEquals(1.60,ci.getLowerBound(), 0.01);
		assertEquals(8.40, ci.getUpperBound(), 0.01);
	}
	
	
	/**
	 * Second sample T distribution Confidence Interval problem from
	 * 
	 * http://onlinestatbook.com/2/estimation/mean.html
	 * 
	 */
	@Test
	void testScenario3() {
		double[] data = new double[] {
				21,43,17,19,15,
				12,25,33,14,8,
				10,9,23,4,24,14,
				13,15,15,25,21,
				16,16,12,28,23,
				21,14,11,9,11,
				10,13,8,11,9,
				16,27,9,12,11,
				25,18,8,15,19,
				17
		};
		
		SampledStatistic m = new SampledStatistic();
		m.add(data);
		
		assertEquals(16.362, m.getMean(), 0.001);
		assertEquals(7.470, m.getStandardDeviation(), 0.001);
		
		ConfidenceInterval ci = m.getConfidenceInterval(0.95);
		assertEquals(14.17, ci.getLowerBound(), 0.01);
		assertEquals(18.56, ci.getUpperBound(), 0.01);
	}
	
}
