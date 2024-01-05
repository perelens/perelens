package com.perelens.simulation.statistic;

import static org.junit.jupiter.api.Assertions.*;

import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Test;

import com.perelens.simulation.statistics.DistributionSamples;
import com.perelens.statistics.ConfidenceInterval;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
 * 
 */
class DistributionSamplesTest {

	/**
	 * Disribution-Free Confidence Intervals for percentiles problem from 
	 * 
	 * https://newonlinecourses.science.psu.edu/stat414/node/317/
	 */
	@Test
	void testScenario1() {
		long[] data = new long[] {
				325,   325,   334,   339,   356,   356,   359,   359,   363,
				364,   364,   366,   369,   370,   373,   373,   374,   375,
				389,   392,   393,   394,   397,   402,   403,   424};
		
		DistributionSamples ds = new DistributionSamples();
		ds.addData(data);
		
		assertEquals(392.25, ds.getPercentile(0.75), 0.01);
		
		ConfidenceInterval ci = ds.getConfidenceInterval(0.75, 0.9341);
		assertEquals(373.0, ci.getLowerBound());
		assertEquals(402.0, ci.getUpperBound());
		
		System.out.println(ci.getConfidenceLevel());
		
		Histogram h = ds.getHistogram();
		
		assertEquals(26,h.getTotalCount());
		assertEquals(2, h.getCountAtValue(325));
		assertEquals(1, h.getCountAtValue(424));
		
		long sum = 0;
		for (long d : data) {
			sum +=d;
		}
		
		assertEquals(((double)sum)/data.length,ds.getMean());
	}

}
