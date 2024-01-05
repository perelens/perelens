/**
 * 
 */
package com.perelens.engine.api;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License

 * 
 * Some events just occur or not, like heads coming up in a coin flip.
 * Other events can also have a magnitude when they do occur, such as an earthquake.
 * 
 * This interface allows events to capture these magnitudes
 * 
 * @author Steve Branda
 *
 */
public interface EventMagnitude {
	
	public interface Unit{
		public String name();
	};
	
	public final static EventMagnitude NO_MAGNITUDE = new EventMagnitude() {

		@Override
		public double magnitude() {
			throw new IllegalStateException("Method calls on NO_MAGNITUDE singleton are invalid.");
		}
		
		public Unit unit() {
			throw new IllegalStateException("Method calls on NO_MAGNITUDE singleton are invalid.");
		}
	};
	
	public final static Unit NO_UNIT = new Unit() {

		@Override
		public String name() {
			return "NO_UNIT";
		}};

	public double magnitude();
	
	public default Unit unit() {
		return NO_UNIT;
	}
}
