/**
 * 
 */
package com.perelens.simulation.api;

import java.io.Serializable;

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
public interface RandomGenerator extends Serializable {
	
	/**
	 * RandomGenerator that always returns 0.5.
	 * For use where the value is ignored.
	 * 
	 */
	public static final RandomGenerator NULL_GENERATOR = new RandomGenerator() {

		private static final long serialVersionUID = -1428761965771854186L;

		@Override
		public double nextDouble() {
			return 0.5;
		}

		@Override
		public String getRandomSetup() {
			return "NULL_GENERATOR";
		}

		@Override
		public RandomGenerator copy() {
			return this;
		}};

	/**
	 * Returns the next double in the range of 0 (inclusive) to 1 (exclusive).
	 * 
	 * @return
	 */
	public double nextDouble();
	
	/**
	 * Returns a String representation of the configuration and initialization of this random number generator
	 * that is sufficient for the RandomProvider implementation to recreate it.
	 * 
	 * @return
	 */
	public String getRandomSetup();
	
	public RandomGenerator copy();
}
