/**
 * 
 */
package com.perelens.simulation.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.perelens.simulation.api.DistributionProvider.Interval;

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
public abstract class DistributionProviderTests {

	protected abstract DistributionProvider getProvider();
	
	@Test
	void testExponentialDistribution() {
		DistributionProvider dp = getProvider();
		Distribution exp = dp.exponential(10000);
		
		double total = 0;
		long count = 0;
		
		for (double i = 0.00000000001; i < 1; i += 1.0d/1000000.0d) {
			total += exp.sample(i);
			count++;
		}
		
		double calcMean = total/count;
		assertEquals(10000,calcMean,2);
	}
	
	@Test
	void testExponentialDistributionPercentileAtValue() {
		DistributionProvider dp = getProvider();
		
		double percentile = 0.2;
		double target = 1000;
		
		Distribution exp = dp.exponential(percentile,target);
		
		
		double count = 0;
		double total = 0;
		
		for (double i = 0.00000000001; i < 1; i += 0.0000001d) {
			if (exp.sample(i) <= target) {
				count++;
			}
			total++;
		}
		
		double calcPercentile = count/total;
		assertEquals(percentile, calcPercentile, 0.00001);
	}
	
	@Test
	void testClosedUniformDistribution() {
		DistributionProvider dp = getProvider();
		Distribution unf = dp.uniform(1, 1000000, Interval.CLOSED);
		
		double total = 0;
		long count = 0;
		
		for (double i = 0.00000000001; i < 1; i += 1.0d/1000000.0d) {
			total += unf.sample(i);
			count++;
		}
		
		double calcMean = total/count;
		assertEquals(500000,calcMean,2);
		
		double out = unf.sample(0);
		assertEquals(1,out);
		
		out = unf.sample(1.0);
		assertEquals(1000000,out);
		
		unf = dp.uniform(-1000000, -1, Interval.CLOSED);
		
		total = 0;
		count = 0;
		
		for (double i = 0.00000000001; i < 1; i += 1.0d/1000000.0d) {
			total += unf.sample(i);
			count++;
		}
		
		calcMean = total/count;
		assertEquals(-500000,calcMean,2);
		
		out = unf.sample(0);
		assertEquals(-1000000,out);
		
		out = unf.sample(1.0);
		assertEquals(-1,out);
	}
	
	@Test
	void testOpenUniformDistribution() {
		DistributionProvider dp = getProvider();
		Distribution unf = dp.uniform(1, 1000000, Interval.OPEN);
		
		double total = 0;
		long count = 0;
		
		for (double i = 0.00000000001; i < 1; i += 1.0d/1000000.0d) {
			total += unf.sample(i);
			count++;
		}
		
		double calcMean = total/count;
		assertEquals(500000,calcMean,2);
		
		double out = unf.sample(0);
		assertTrue(1 < out && out < 1.1);
		
		out = unf.sample(1.0);
		assertTrue(1000000 > out && out > 999999.9);
		
		unf = dp.uniform(-1000000, -1, Interval.OPEN);
		
		total = 0;
		count = 0;
		
		for (double i = 0.00000000001; i < 1; i += 1.0d/1000000.0d) {
			total += unf.sample(i);
			count++;
		}
		
		calcMean = total/count;
		assertEquals(-500000,calcMean,2);
		
		out = unf.sample(0);
		assertTrue(-1000000 < out && out < -999999.9);
		
		out = unf.sample(1.0);
		assertTrue(-1 > out && out > -1.1);
	}
	
	@Test
	void testExtraLargeRangeClosedUniformDistribution() {
		double lower = -Double.MAX_VALUE;
		double upper = Double.MAX_VALUE;
		
		DistributionProvider dp = getProvider();
		Distribution unf = dp.uniform(lower, upper, Interval.CLOSED);
		
		double d = unf.sample(0);
		assertEquals(-Double.MAX_VALUE, d);
		
		d = unf.sample(1);
		assertEquals(Double.MAX_VALUE,d);
		
		d = unf.sample(0.5);
		assertEquals(0,d,2);		
	}
	
	@Test
	void testExtraLargeRangeOpenUniformDistribution() {
		double lower = -Double.MAX_VALUE;
		double upper = Double.MAX_VALUE;
		
		DistributionProvider dp = getProvider();
		Distribution unf = dp.uniform(lower, upper, Interval.OPEN);
		
		double d = unf.sample(0);
		assertEquals(-Double.MAX_VALUE + Math.ulp(Double.MAX_VALUE), d);
		
		d = unf.sample(1);
		assertTrue(Double.MAX_VALUE > d);
		assertTrue((Double.MAX_VALUE - d)/Double.MAX_VALUE < 0.00001);
		
		d = unf.sample(0.5);
		assertEquals(0,d,Math.ulp(Double.MAX_VALUE));
		
		for (double ur = 0.01; ur <=1.0; ur += 0.01) {
			assertTrue(unf.sample(ur) > unf.sample(ur - 0.01));
		}
	}
	
	@Test
	void testLognormalDistribution() {
		
		double m = 13.7;
		double s = 1.33;
		
		DistributionProvider dp = getProvider();
		Distribution ln = dp.lognormal(m, s);
		
		double total = 0;
		long count = 0;
		
		for (double i = Double.MIN_NORMAL; i < 1; i += 0.000000007) {
			total += ln.sample(i);
			count++;
		}
		
		double calcMean = total/count;
		
		double mean = Math.exp(m + (s*s)/2);
		assertEquals(mean,calcMean,2);
		
	}
	
	@Test
	void testLognormalDistribution90CI() {
		
		double ub = 8_000_000;
		double lb = 100_000;
		
		DistributionProvider dp = getProvider();
		Distribution ln = dp.lognormal90pctCI(lb, ub);
		
		double total = 0;
		long count = 0;
		
		for (double i = Double.MIN_NORMAL; i < 1; i += 0.000000007) {
			total += ln.sample(i);
			count++;
		}
		
		double calcMean = total/count;
		
		double lnUB = Math.log(ub);
		double lnLB = Math.log(lb);
		
		double m = (lnUB + lnLB)/2.0d;
		double s = (lnUB - lnLB)/3.28971d;
		double mean = Math.exp(m + (s*s)/2);
		
		assertEquals(mean,calcMean,2);
		
	}

}
