/**
 * 
 */
package com.perelens.simulation.failure;

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
class FailureUtilsTest {

	/**
	 * Test method for {@link com.perelens.simulation.failure.FailureUtils#processIsSuccessful(double, double)}.
	 */
	@Test
	void testProcessIsSuccessful() {
		
		for (double i = 0.0; i < 1.0; i += 0.01) {
			assertFalse(FailureUtils.processIsSuccessful(0.0, i)); //Should always be false because 0% chance of success
		}
		
		for (double i = 0.0; i < 1.0; i += 0.01) {
			assertTrue(FailureUtils.processIsSuccessful(1.0, i)); //Should always be true because 100% chance of success
		}
		
		double successes = 0;
		double total = 0;
		
		for (double i = 0.0; i < 1.0; i += 0.01) {
			if (FailureUtils.processIsSuccessful(0.4,i)) {
				successes++;
			}
			total++;
		}
		
		assertEquals(0.4,successes/total,0.0001);
		
	}

	/**
	 * Test method for {@link com.perelens.simulation.failure.FailureUtils#processFails(double, double)}.
	 */
	@Test
	void testProcessFails() {
		for (double i = 0.0; i < 1.0; i += 0.01) {
			assertFalse(FailureUtils.processFails(0.0, i)); //Should always be false because 0% chance of failure
		}
		
		for (double i = 0.0; i < 1.0; i += 0.01) {
			boolean val = FailureUtils.processFails(1.0, i);
			if (!val) {
				System.out.println();
			}
			assertTrue(val); //Should always be true because 100% chance of failure
		}
		
		double fails = 0;
		double total = 0;
		
		for (double i = 0.0; i < 1.0; i += 0.01) {
			if (FailureUtils.processFails(0.4,i)) {
				fails++;
			}
			total++;
		}
		
		assertEquals(0.4,fails/total,0.0001);
		
		fails = 0;
		total = 0;
		for (double i = 0.0; i < 1.0; i += 0.001) {
			if (FailureUtils.processFails(0.04,i)) {
				fails++;
			}
			total++;
		}
		
		assertEquals(0.04,fails/total,0.0001);
		
		fails = 0;
		total = 0;
		for (double i = 0.0; i < 1.0; i += 0.0001) {
			if (FailureUtils.processFails(0.004,i)) {
				fails++;
			}
			total++;
		}
		
		assertEquals(0.004,fails/total,0.0001);
	}

	/**
	 * Test method for {@link com.perelens.simulation.failure.FailureUtils#checkUniformRandom(double)}.
	 */
	@Test
	void testCheckUniformRandom() {
		try {
			FailureUtils.checkUniformRandom(-0.1);
			fail();
		}catch(IllegalArgumentException e) {
		}
		try {
			FailureUtils.checkUniformRandom(1.1);
			fail();
		}catch(IllegalArgumentException e) {
		}
		try {
			FailureUtils.checkUniformRandom(1.0);
			fail();
		}catch(IllegalArgumentException e) {
		}
		
		for (double i = 0.0; i < 1.0; i+=0.01) {
			FailureUtils.checkUniformRandom(i);
		}
	}

}
