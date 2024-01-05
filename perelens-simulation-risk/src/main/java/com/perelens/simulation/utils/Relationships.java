/**
 * 
 */
package com.perelens.simulation.utils;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * Static method utility class based on established availability relationships.
 * 
 * @author Steve Branda
 *
 */
public class Relationships {

	
	public static double getMeanTimeBetweenFailure(double availability, double meanTimeToRepair) {
		checkAvailability(availability);
		checkMTR(meanTimeToRepair);
		
		return (availability * meanTimeToRepair)/(1 - availability);
	}
	
	public static double getMeanTimeToRepair(double availability, double meanTimeBetweenFailure) {
		checkAvailability(availability);
		checkMTBF(meanTimeBetweenFailure);
		
		return (meanTimeBetweenFailure/availability - meanTimeBetweenFailure);
	}
	
	public static double getAvailability(double meanTimeBetweenFailure, double meanTimeToRepair) {
		return meanTimeBetweenFailure/(meanTimeBetweenFailure + meanTimeToRepair);
	}
	
	protected static void checkAvailability(double availability) {
		if (availability < 0d || availability > 1d) {
			throw new IllegalArgumentException("Availability must be between 0.0 and 1.0");
		}
	}
	
	protected static void checkMTR(double repairTime) {
		if (repairTime < 0) {
			throw new IllegalArgumentException("Mean Time to Repair must be greater than or equal to 0");
		}
	}
	
	protected static void checkMTBF(double mtbf) {
		if (mtbf < 0) {
			throw new IllegalArgumentException("Mean Time to Failure must be greater than or equal to 0");
		}
	}
}
