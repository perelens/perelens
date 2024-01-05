package com.perelens.engine.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

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
 */
class UtilsTest {

	@Test
	void testAppend() {
		String two = "2";
		String[] test = new String[] {"1","3","4","5",two};
		
		int eIndex = 5;
		for (int i = 0; i < eIndex - 1; i++) {
			if (test[i] == two) {
				System.out.println(Arrays.toString(Arrays.copyOfRange(test, i+1, eIndex)));
			}
		}
	}

	@Test
	void testCompress() {
		
		//Test on every possible combination of an even length array
		String[] toTest = new String[4];
		for (int i = 0; i <= 1; i++) {
			toTest[0] = i == 0?null:"1";
			
			for (int j = 0; j <= 1; j++) {
				toTest[1] = j==0?null:"10";
				
				for (int k = 0; k <=1; k++) {
					toTest[2] = k==0?null:"100";
					
					for (int m = 0; m <=1; m++) {
						toTest[3] = m==0?null:"1000";
						
						int expected = 0;
						for (int p = 0; p < toTest.length; p ++) {
							if (toTest[p] != null) {
								expected += Integer.parseInt(toTest[p]);
							}
						}
						
						int index = 4;
						do {
							String[] tc = Arrays.copyOf(toTest, toTest.length);
							Utils.compress(tc, index);
							
							int found = 0;
							for (int p = 0; p < toTest.length; p ++) {
								if (tc[p] != null) {
									found += Integer.parseInt(tc[p]);
								}
							}
							
							assertEquals(expected,found,Arrays.toString(toTest));
						}while(index > 0 && toTest[--index] == null);
					}
				}
			}
		}
		
		//Test on every possible combination of an odd length array
		toTest = new String[5];
		for (int i = 0; i <= 1; i++) {
			toTest[0] = i == 0?null:"1";
			
			for (int j = 0; j <= 1; j++) {
				toTest[1] = j==0?null:"10";
				
				for (int k = 0; k <=1; k++) {
					toTest[2] = k==0?null:"100";
					
					for (int m = 0; m <=1; m++) {
						toTest[3] = m==0?null:"1000";
						
						for (int n = 0; n <=1; n++) {
							toTest[4] = n==0?null:"10000";
							
							int expected = 0;
							for (int p = 0; p < toTest.length; p ++) {
								if (toTest[p] != null) {
									expected += Integer.parseInt(toTest[p]);
								}
							}
							
							int index = 5;
							do {
								String[] tc = Arrays.copyOf(toTest, toTest.length);
								Utils.compress(tc, index);
								
								int found = 0;
								for (int p = 0; p < toTest.length; p ++) {
									if (tc[p] != null) {
										found += Integer.parseInt(tc[p]);
									}
								}
								
								assertEquals(expected,found,Arrays.toString(toTest));
							}while(index > 0 && toTest[--index] == null);
						}
					}
				}
			}
		}
	}

	@Test
	void testCheckPercentage() {
		try {
			Utils.checkPercentage(-0.1);
			fail();
		}catch(IllegalArgumentException e) {
		}
		try {
			Utils.checkPercentage(1.1);
			fail();
		}catch(IllegalArgumentException e) {
		}
		
		for (double i = 0.0; i <= 1.0; i+=0.01) {
			Utils.checkPercentage(i);
		}
	}
}
