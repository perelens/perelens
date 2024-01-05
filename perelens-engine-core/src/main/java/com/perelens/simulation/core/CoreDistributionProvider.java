/**
 * 
 */
package com.perelens.simulation.core;

import org.apache.commons.math3.special.Erf;

import com.perelens.engine.utils.Utils;
import com.perelens.simulation.api.Distribution;
import com.perelens.simulation.api.DistributionProvider;

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
public class CoreDistributionProvider implements DistributionProvider {

	@Override
	public Distribution exponential(double mean) {
		return new ExponentialDistribution(mean);
	}
	
	@Override
	public Distribution exponential(double percentile, double atValue) {
		if (percentile <= 0.0 || percentile >= 1.0) {
			throw new IllegalArgumentException(SimMsgs.valueMustBeBetween(0.0, 1.0, percentile));
		}
		Utils.checkArgStrictlyPositive(atValue);
		
		double mean = -atValue/Math.log(1.0d - percentile);
		return exponential(mean);
	}
	
	@Override
	public Distribution uniform(double lower, double upper, Interval intervalType) {
		return new UniformDistribution(lower,upper,intervalType);
	}
	
	@Override
	public Distribution lognormal(double mean, double standardDeviation) {
		return new LognormalDistribution(mean,standardDeviation);
	}
	
	@Override
	public Distribution lognormal90pctCI(double lowerBound, double upperBound) {
		Utils.checkArgStrictlyPositive(lowerBound);
		Utils.checkArgStrictlyPositive(upperBound);
		if (lowerBound >= upperBound) {
			throw new IllegalArgumentException(SimMsgs.upperBoundMustBeGreater(upperBound, lowerBound));
		}
		
		double lnUB = Math.log(upperBound);
		double lnLB = Math.log(lowerBound);
		
		double mean = (lnUB + lnLB)/2.0d;
		double stdDev = (lnUB - lnLB)/3.28971d;
		
		return lognormal(mean,stdDev);
	}
	
	@Override
	public Distribution getDistribution(String setup) {
		Utils.checkNull(setup);
		
		if (setup.startsWith(ExponentialDistribution.SETUP_NAME)) {
			return ExponentialDistribution.getExponentialDistribution(setup);
		}
		
		//TODO finish this up
		
		throw new IllegalArgumentException(SimMsgs.unknownDistributionSpec(setup));
	}
	
	private static class UniformDistribution implements Distribution{

		private static final long serialVersionUID = 7465451124835944546L;

		protected static final String SETUP_NAME = "Uniform;";
		
		private DistributionProvider.Interval intervalType;
		private double lower;
		private double upper;
		private double a;
		private double b;
		private boolean rangeSpansZero = false;
		
		protected UniformDistribution(double lower, double upper, Interval iType) {
			if (lower >= upper) {
				throw new IllegalArgumentException(SimMsgs.upperBoundMustBeGreater(upper, lower));
			}
			this.lower = lower;
			this.upper = upper;
			
			if (upper > 0.0 && lower < 0.0) {
				rangeSpansZero = true;
			}
			
			intervalType = iType;
			
			if (intervalType == Interval.OPEN) {
				double ulp = Math.max(Math.ulp(lower), Math.ulp(upper));
				a = lower + ulp;
				b = upper - ulp - ulp;
			}else {
				a = lower;
				b = upper;
			}
		}
		
		@Override
		public String getSetup() {
			StringBuilder toReturn = new StringBuilder(SETUP_NAME);
			toReturn.append(lower).append(';').append(upper);
			toReturn.append(';').append(intervalType.name());
			return toReturn.toString();
		}

		@Override
		public Distribution copy() {
			return this;  //Distribution is stateless and not mutable, so just return the same object
		}

		@Override
		public double sample(double uniformRandom) {
			double tr;
			if (rangeSpansZero) {
				tr = ((uniformRandom * b) + a) - uniformRandom*a;
			}else {
				tr = (uniformRandom * (b-a)) + a;
			}
			
			return tr;
		}
	}
	
	
	
	private static class ExponentialDistribution implements Distribution{
		
		protected static final String SETUP_NAME = "Exponential;";

		private static final long serialVersionUID = -4933388153353146851L;

		private double negMean;
		
		protected static ExponentialDistribution getExponentialDistribution(String setup) {
			String[] pieces = setup.split("\\;");
			if (Integer.parseInt(pieces[0]) != 2 || !pieces[0].equals(SETUP_NAME)) {
				throw new IllegalArgumentException(SimMsgs.badSetupArgument(setup));
			}
			
			double mean = Double.parseDouble(pieces[1]);
			return new ExponentialDistribution(mean);
		}
		
		protected ExponentialDistribution(double mean) {
			if (mean <= 0.0) throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(mean));
			this.negMean = -mean;
		}
		
		@Override
		public double sample(double uniformRandom) {
			double nextran = negMean * Math.log(uniformRandom);
			return nextran;
		}
		
		protected Object readResolve() {
			return new ExponentialDistribution(-negMean);
		}

		@Override
		public String getSetup() {
			StringBuilder toReturn = new StringBuilder(SETUP_NAME);
			toReturn.append(-negMean);
			return toReturn.toString();
		}

		@Override
		public Distribution copy() {
			return this; //Exponential distribution is stateless and not mutable, so just return the same object
		}
	}
	
	private static class LognormalDistribution implements Distribution{

		private static final long serialVersionUID = -9036555600493592565L;

		protected static final String SETUP_NAME = "Lognormal;";
		
		private double mean;
		private double devTerm;
		
		
		protected LognormalDistribution(double mean, double stdDev) {
			if (mean <= 0.0) throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(mean));
			if (stdDev <= 0.0) throw new IllegalArgumentException(SimMsgs.mustBeStrictlyPositive(stdDev));
			
			this.mean = mean;
			devTerm = Math.sqrt(2.0d * stdDev * stdDev);
		}

		@Override
		public double sample(double uniformRandom) {
			return Math.exp(mean + devTerm * Erf.erfInv(2 * uniformRandom - 1));
		}

		@Override
		public String getSetup() {
			StringBuilder toReturn = new StringBuilder(SETUP_NAME);
			toReturn.append(mean);
			toReturn.append(';').append(devTerm);
			return toReturn.toString();
		}

		@Override
		public Distribution copy() {
			//stateless and not mutable
			return this;
		}
	}
	
	private static class ConstantDistribution implements Distribution{

		private static final long serialVersionUID = -1418074249176502936L;

		protected static final String SETUP_NAME = "Constant;";
		
		private double v;
		
		protected ConstantDistribution (double value) {
			v = value;
		}
		
		@Override
		public double sample(double uniformRandom) {
			return v;
		}

		@Override
		public String getSetup() {
			StringBuilder toReturn = new StringBuilder(SETUP_NAME);
			toReturn.append(v);
			return toReturn.toString();
		}

		@Override
		public Distribution copy() {
			return this;
		}
	}

	@Override
	public Distribution constant(double value) {
		return new ConstantDistribution(value);
	}
}

	