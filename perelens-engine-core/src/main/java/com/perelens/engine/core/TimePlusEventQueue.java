/**
 * 
 */
package com.perelens.engine.core;

import java.util.Arrays;
import java.util.Comparator;

import com.perelens.engine.api.Event;
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
public class TimePlusEventQueue extends TimeQueue {

	private static final Event[] EMPTY_HEAP = new Event[0];
	
	private Event[] minheap = EMPTY_HEAP;
	private int count = 0;
	private int capacity = 0;
	private Comparator<Event> comparator;
	
	protected void setComparator (Comparator<Event> comp) {
		Utils.checkNull(comp);
		this.comparator = comp;
	}
	
	protected void ev_enqueue(Event val) {
		Utils.checkNull(val);
		if (count == capacity) {
			if (capacity == 0) {
				capacity = 4;
				minheap = new Event[capacity + 1];
			}else {
				capacity = capacity + capacity;
				
				Event[] temp = new Event[capacity + 1];
				System.arraycopy(minheap, 1, temp, 1, count);
				minheap = temp;
			}	
		}
		
		count++;
		minheap[count] = val;
		ev_bubbleUp(count);
	}
	
	protected Event ev_dequeue() {
		if (count == 0) {
			return null;
		}
		
		Event toReturn = minheap[1];
		minheap[1] = minheap[count];
		minheap[count] = null;
		count--;
		ev_sinkDown(1);
		
		return toReturn;
	}
	
	protected Event ev_peek() {
		if (count > 0) {
			return minheap[1];
		}else {
			return null;
		}
	}
	
	protected boolean ev_hasMore() {
		return count > 0;
	}
	
	protected void syncInternalState(TimePlusEventQueue toSync) {
		Utils.checkNull(toSync);
		super.syncInternalState(toSync);
		if (this.minheap == EMPTY_HEAP) {
			toSync.minheap = EMPTY_HEAP;
		}else {
			toSync.minheap = Arrays.copyOf(this.minheap, this.minheap.length);
		}
		toSync.count = this.count;
		toSync.capacity = this.capacity;
		toSync.comparator = this.comparator;
	}
	
	
	private final void ev_swap(int f, int t) {
		Event temp = minheap[f];
		minheap[f] = minheap[t];
		minheap[t] = temp;
	}
	
	private final void ev_bubbleUp(int pos) {
		int parent = parent(pos);
		int current = pos;
		
		while(parent > 0 && comparator.compare(minheap[parent],minheap[current]) > 0 ){
			ev_swap(current,parent);
			current = parent;
			parent = parent(parent);
		}
	}
	
	private final void ev_sinkDown(int pos) {
		int small = pos;
		int lchild = leftChild(pos);
		int rchild = rightChild(pos);
		
		if (lchild <= count && comparator.compare(minheap[small],minheap[lchild] ) > 0) {
			small = lchild;
		}
		
		if (rchild <= count && comparator.compare(minheap[small],minheap[rchild]) > 0 ){
			small = rchild;
		}
		
		if (small != pos) {
			ev_swap(pos,small);
			ev_sinkDown(small);
		}
	}
}
