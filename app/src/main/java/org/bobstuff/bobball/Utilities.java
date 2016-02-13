/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class Utilities extends Activity {

	public static void arrayCopy(int[][] source,int[][] destination) {
		for (int a=0; a < source.length; ++a) {
			System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
		}
	}

	public static ArrayAdapter createDropdown (Context context, int count) {
		List<String> dropdownItems = new ArrayList<>();

		for (int i = 1; i <= count; i++) { dropdownItems.add("" + i); }

		ArrayAdapter adapter = new ArrayAdapter(context,android.R.layout.simple_spinner_item, dropdownItems);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		return adapter;
	}

	public static ArrayAdapter createDropdownFromStrings (Context context, int stringsId){
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, stringsId, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}
}
