/**
 * 
 */
package com.perelens.statistics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
class LogLikeRatioTest {

	/**
	 * Test method for {@link resilience.statistics.LogLikelihoodRatio#binomialConfidenceInterval(long, long)}.
	 */
	@Test
	void testBinomialConfidenceIntervalLongLong() {
		
		ConfidenceInterval ci = LogLikeRatio.binomialConfidenceInterval(1315, 630);
		
		assertEquals(0.452,ci.getLowerBound(),0.0005);
		assertEquals(0.506,ci.getUpperBound(),0.0005);
		assertEquals(0.95,ci.getConfidenceLevel());
		
		ci = LogLikeRatio.binomialConfidenceInterval(10, 1);
		
		assertEquals(0.006, ci.getLowerBound(),0.0005);
		assertEquals(0.372, ci.getUpperBound(),0.0005);
		assertEquals(0.95,ci.getConfidenceLevel());
	}

	/**
	 * Test method for {@link resilience.statistics.LogLikelihoodRatio#binomialConfidenceInterval(long, long, double)}.
	 */
	@Test
	void testBinomialConfidenceIntervalLongLongDouble() {
		ConfidenceInterval ci = LogLikeRatio.binomialConfidenceInterval(1315, 630);
		
		assertEquals(0.452,ci.getLowerBound(),0.0005);
		assertEquals(0.506,ci.getUpperBound(),0.0005);
		assertEquals(0.95,ci.getConfidenceLevel());
		
		ci = LogLikeRatio.binomialConfidenceInterval(10, 1);
		
		assertEquals(0.006, ci.getLowerBound(),0.0005);
		assertEquals(0.372, ci.getUpperBound(),0.0005);
		assertEquals(0.95,ci.getConfidenceLevel());
	}

}
