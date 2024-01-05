/**
 * 
 */
package com.perelens.engine.core;

import java.util.Arrays;

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
class TimeCallbackQueue {
	
	private static final long[] EMPTY_HEAP = new long[0];
	
	private long[] minheap = EMPTY_HEAP;
	private int count = 0;
	private int capacity = 0;
	
	protected void tc_enqueue(long val) {
		if (count == capacity) {
			if (capacity == 0) {
				capacity = 4;
				minheap = new long[capacity + 1];
			}else {
				capacity = capacity + capacity;
				
				long[] temp = new long[capacity + 1];
				System.arraycopy(minheap, 1, temp, 1, count);
				minheap = temp;
			}	
		}
		
		count++;
		minheap[count] = val;
		tc_bubbleUp(count);
	}
	
	protected long tc_dequeue() {
		if (count == 0) {
			return -1;
		}
		
		long toReturn = minheap[1];
		minheap[1] = minheap[count];
		minheap[count] = 0;
		count--;
		tc_sinkDown(1);
		return toReturn;
	}
	
	protected long tc_peek() {
		if (count > 0) {
			return minheap[1];
		}else {
			return -1;
		}
	}
	
	protected boolean tc_hasMore() {
		return count > 0;
	}
	
	protected void syncInternalState(TimeCallbackQueue toSync) {
		Utils.checkNull(toSync);
		if (this.minheap == EMPTY_HEAP) {
			toSync.minheap = EMPTY_HEAP;
		}else {
			toSync.minheap = Arrays.copyOf(this.minheap, this.minheap.length);
		}
		toSync.count = this.count;
		toSync.capacity = this.capacity;
	}
	
	//heap management functions
	protected final static int parent(int position) {
		return position >>> 1;
	}
	
	protected final static int leftChild(int position) {
		return position << 1;
	}
	
	protected final static int rightChild(int position) {
		return (position << 1) + 1;
	}
	
	private final void tc_swap(int f, int t) {
		long temp = minheap[f];
		minheap[f] = minheap[t];
		minheap[t] = temp;
	}
	
	private final void tc_bubbleUp(int pos) {
		int parent = parent(pos);
		int current = pos;
		
		while(parent > 0 && minheap[parent] > minheap[current]) {
			tc_swap(current,parent);
			current = parent;
			parent = parent(parent);
		}
	}
	
	private final void tc_sinkDown(int pos) {
		int small = pos;
		int lchild = leftChild(pos);
		int rchild = rightChild(pos);
		
		if (lchild <= count && minheap[small] > minheap[lchild]) {
			small = lchild;
		}
		
		if (rchild <= count && minheap[small] > minheap[rchild]) {
			small = rchild;
		}
		
		if (small != pos) {
			tc_swap(pos,small);
			tc_sinkDown(small);
		}
	}
}
