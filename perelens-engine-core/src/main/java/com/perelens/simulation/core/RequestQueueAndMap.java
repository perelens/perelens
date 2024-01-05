/**
 * 
 */
package com.perelens.simulation.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.perelens.engine.api.Event;
import com.perelens.engine.core.AbstractEventResponder;
import com.perelens.engine.utils.Utils;

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
abstract class RequestQueueAndMap extends AbstractEventResponder{

	public RequestQueueAndMap(String id) {
		super(id);
	}

	//FIFO queue implementation
	private final static String[] EMPTY_QUEUE = new String[0];

	private String[] queue = EMPTY_QUEUE;
	private int head = 0;
	private int tail = 0;
	private int qCount = 0;
	
	
	protected void r_enqueue(String val) {
		if (queue == EMPTY_QUEUE) {
			queue = new String[4];
		} else if (qCount == queue.length){
			String[] target = new String[queue.length + queue.length];
			if (tail > head) {
				System.arraycopy(queue, head, target, 0, qCount);
			}else {
				int headToEndLen = queue.length - head;
				System.arraycopy(queue, head, target, 0, headToEndLen );
				System.arraycopy(queue, 0, target, headToEndLen, tail);
				head = 0;
				tail = qCount;
			}
			queue = target;
		}
		
		queue[tail] = val;
		tail = (tail + 1) % queue.length;
		qCount++;
	}
	
	protected String r_dequeue() {
		if (qCount == 0) {
			throw new IllegalStateException(SimMsgs.badState());
		}
		String toReturn = queue[head];
		head = (head + 1) % queue.length;
		qCount--;
		return toReturn;
	}
	
	protected String r_peek() {
		if (qCount == 0) {
			throw new IllegalStateException(SimMsgs.badState());
		}else {
			return queue[head];
		}
	}
	
	protected int r_size() {
		return qCount;
	}
	
	//TODO Upgrade to hashing if necessary
	//Map implementation
	private final static Object[] EMPTY_MAP = new Object[0];
	private Object[] map = EMPTY_MAP;
	private int mIndex = 0;
	private int mCount = 0;
	
	protected Event m_put(String key, Event value) {
		Utils.checkNull(key);
		Utils.checkNull(value);
		//First try to replace
		int emptyIndex = -1;
		for (int i = 0; i < mIndex; i += 2) {
			Object k = map[i];
			if (k == null) {
				emptyIndex = i;
			}else if (key.equals(k)) {
				int index = i + 1;
				Object toReturn = map[index];
				map[index] = value;
				return (Event) toReturn;
			}
		}
		
		//Not in the map so insert at the end
		if (emptyIndex == -1) {
			//map is full so increase the size
			if (mIndex == map.length) {
				if (map.length == 0) {
					map = new Object[8];
				}else {
					Object[] temp = new Object[map.length + map.length];
					System.arraycopy(map, 0, temp, 0, mIndex);
					map = temp;
				}
			}
			
			map[mIndex] = key;
			map[mIndex + 1] = value;
			mIndex += 2;
		}else {
			//Insert at the empty Index
			map[emptyIndex] = key;
			map[emptyIndex + 1] = value;
		}
		
		mCount++;
		
		return null;
	}
	
	protected Event m_get(String key) {
		Utils.checkNull(key);
		for (int i = 0; i < mIndex; i += 2) {
			Object k = map[i];
			if (k != null && key.equals(k)) {
				return (Event) map[i + 1];
			}
		}
		return null;
	}
	
	protected Event m_remove(String key) {
		Utils.checkNull(key);
		for (int i = 0; i < mIndex; i += 2) {
			Object k = map[i];
			if (k != null && key.equals(k)) {
				int index = i + 1;
				Event toReturn = (Event) map[index];
				map[i] = null;
				map[index] = null;
				mCount--;
				return toReturn;
			}
		}
		return null;
	}
	
	protected int m_size() {
		return mCount;
	}
	
	protected Iterator<Map.Entry<String,Event>> m_iterator(){
		return new MapIterator();
	}
	
	private class MapIterator implements Iterator<Map.Entry<String,Event>>,Map.Entry<String,Event>{

		private int visited = 0;
		private int current = -2;
		private int count = mCount;
		
		@Override
		public String getKey() {
			return (String) map[current];
		}

		@Override
		public Event getValue() {
			return (Event) map[current + 1];
		}

		@Override
		public Event setValue(Event value) {
			Event toReturn = (Event) map[current+1];
			map[current+1] = value;
			if (value == null) {
				map[current] = null;
				mCount--;
			}
			return toReturn;
		}

		@Override
		public boolean hasNext() {
			return visited < count;
		}

		@Override
		public Entry<String, Event> next() {
			while(current < mIndex) {
				current+=2;
				if (map[current] != null) {
					visited++;
					return this;
				}
			}
			
			throw new IllegalStateException(SimMsgs.badState());
		}
		
	}
	
	protected void syncInternalState(RequestQueueAndMap toSync) {
		Utils.checkNull(toSync);
		super.syncInternalState(toSync);
		toSync.head = this.head;
		toSync.tail = this.tail;
		toSync.qCount = this.qCount;
		if (this.queue == EMPTY_QUEUE) {
			toSync.queue = EMPTY_QUEUE;
		}else {
			toSync.queue = Arrays.copyOf(this.queue, this.queue.length);
		}
		
		toSync.mIndex = this.mIndex;
		if (this.map == EMPTY_MAP) {
			toSync.map = EMPTY_MAP;
		}else {
			toSync.map = Arrays.copyOf(this.map, this.map.length);
		}
	}	
}
