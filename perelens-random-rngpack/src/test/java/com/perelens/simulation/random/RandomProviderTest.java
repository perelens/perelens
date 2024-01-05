package com.perelens.simulation.random;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.perelens.simulation.api.RandomGenerator;
import com.perelens.simulation.api.RandomProvider;

/**
 * Copyright 2020-2023 Steven Branda
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing 
   permissions and limitations under the License
   
   
 * 
 */
public class RandomProviderTest {

	protected RandomProvider getProvider() {
		return new RanluxProvider(System.currentTimeMillis());
	}
	
	@Test
	protected void testCopy() {
		RandomProvider p1 = getProvider();
		RandomProvider p2 = p1.copy();
		
		RandomGenerator p1Gen = p1.createGenerator();
		RandomGenerator p2Gen = p2.createGenerator();
		
		for (int i = 0; i < 10; i++) {
			assertEquals(p1Gen.nextDouble(), p2Gen.nextDouble());
		}
		
		RandomGenerator p1GenCopy = p1Gen.copy();
		RandomGenerator p2GenCopy = p2Gen.copy();
		double[] next100 = new double[100];
		
		for (int i = 0; i < 100; i++) {
			next100[i] = p1GenCopy.nextDouble();
			assertEquals(next100[i], p2Gen.nextDouble());
		}
		
		for (int i = 0; i < 10; i++) {
			assertEquals(next100[i], p1Gen.nextDouble());
		}
		
		for (int i = 0; i < 10; i++) {
			assertEquals(next100[i], p2GenCopy.nextDouble());
		}
	}
	
	@Test
	protected void testSerialize() {
		RandomProvider p1 = getProvider();
		RandomProvider p2 = copy(p1);
		
		RandomGenerator p1Gen = p1.createGenerator();
		RandomGenerator p2Gen = p2.createGenerator();
		
		for (int i = 0; i < 10; i++) {
			assertEquals(p1Gen.nextDouble(), p2Gen.nextDouble());
		}
		
		RandomGenerator p1GenCopy = copy(p1Gen);
		RandomGenerator p2GenCopy = copy(p2Gen);
		
		double[] next100 = new double[100];
		
		for (int i = 0; i < 100; i++) {
			next100[i] = p1GenCopy.nextDouble();
			assertEquals(next100[i], p2Gen.nextDouble());
		}
		
		for (int i = 0; i < 100; i++) {
			assertEquals(next100[i], p1Gen.nextDouble());
		}
		
		for (int i = 0; i < 100; i++) {
			assertEquals(next100[i], p2GenCopy.nextDouble());
		}
	}
	
	@Test
	protected void testRandomization() {
		RandomProvider p1 = getProvider();
		RandomGenerator p1gen = p1.createGenerator();
		
		HashSet<Double> hist = new HashSet<>();
		
		long dupCount = 0;
		for (int i = 0; i < 10000; i++) {
			if (!hist.add(p1gen.nextDouble())) {
				dupCount++;
			}
		}
		
		assertTrue(dupCount < 100, "" + dupCount);
		System.out.println(dupCount);
	}
	
	
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> T copy(T toCopy) {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(bout);
			oout.writeObject(toCopy);
			oout.close();
			
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			ObjectInputStream oin = new ObjectInputStream(bin);
			T toReturn = (T) oin.readObject();
			oin.close();
			return toReturn;
		} catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException("Should not occur", e);
		}
	}
	

}
