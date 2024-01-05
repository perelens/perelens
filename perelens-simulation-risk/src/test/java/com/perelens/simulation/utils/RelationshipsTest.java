/**
 * 
 */
package com.perelens.simulation.utils;

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
class RelationshipsTest {

	@Test
	void getMeanTimeBetweenFailure() {
		double mtbf = 900;
		double mtr  = 100;
		
		double availability = mtbf / (mtbf + mtr);
		
		assertEquals(mtbf, Relationships.getMeanTimeBetweenFailure(availability,mtr), 0.0000001);
	}

}
