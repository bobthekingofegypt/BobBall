/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

public class Utilities {

	public static void arrayCopy(int[][] source,int[][] destination) {
		for (int a=0;a<source.length;a++) {
			System.arraycopy(source[a],0,destination[a],0,source[a].length);
		}
	}
}
