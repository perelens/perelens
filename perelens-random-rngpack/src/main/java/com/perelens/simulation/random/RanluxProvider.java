/**
 * 
 */
package com.perelens.simulation.random;

import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;

import edu.cornell.lassp.houle.RngPack.Ranlux;

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
public class RanluxProvider implements RandomProvider {

	private static final long serialVersionUID = 2798174781841308110L;
	
	private long startSeed;
	private long currentSeed;
	
	public RanluxProvider (long startSeed) {
		this.startSeed = startSeed;
		this.currentSeed = startSeed;
	}
	
	protected RanluxProvider(long startSeed,long currentSeed) {
		this.startSeed = startSeed;
		this.currentSeed = currentSeed;
	}
	
	@Override
	public RandomGenerator createGenerator() {
		var toReturn = new RanluxGenerator(currentSeed++);
		//Need to generate some numbers in order to guarantee randomness
		//of the first generated number
		toReturn.nextDouble();
		long count = currentSeed % 7;
		for (int i = 0; i < count; i++) {
			toReturn.nextDouble();
		}
		return toReturn;
	}

	@Override
	public RandomGenerator createGenerator(String generatorSetup) {
		String[] pieces = generatorSetup.split("\\;");
		if (Integer.parseInt(pieces[1]) != 4 ||
			!"Ranlux".equals(pieces[0])) {
			throw new IllegalArgumentException("Bad setup string");
		}
		
		long seed = Long.parseLong(pieces[2]);
		return new RanluxGenerator(seed);
	}

	@Override
	public String getSetup() {
		StringBuilder toReturn = new StringBuilder("Ranlux;");
		toReturn.append(Ranlux.maxlev).append(';').append(startSeed);
		return toReturn.toString();
	}
	
	@Override
	public RandomProvider copy() {
		return new RanluxProvider(startSeed,currentSeed);
	}
	
	protected static class RanluxGenerator implements RandomGenerator{

		private static final long serialVersionUID = 5562338309410812780L;
		
		private Ranlux rng;
		private long seed;
		
		protected RanluxGenerator(long seed) {
			this.seed = seed;
			rng = new Ranlux(Ranlux.maxlev, seed);
		}
		
		protected RanluxGenerator() {};
		
		@Override
		public double nextDouble() {
			return rng.raw();
		}

		@Override
		public String getRandomSetup() {
			StringBuilder toReturn = new StringBuilder("Ranlux;");
			toReturn.append(Ranlux.maxlev).append(';').append(seed);
			return toReturn.toString();
		}

		@Override
		public RandomGenerator copy() {
			RanluxGenerator toReturn = new RanluxGenerator();
			toReturn.seed = seed;
			toReturn.rng = new Ranlux(rng);
			return toReturn;
		}
	}
}
