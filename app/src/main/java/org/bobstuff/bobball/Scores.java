/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Scores {
	private static final String SCORE_SEPERATOR = "~~";
	private static final String ENTRY_SEPERATOR = "::";
	private List<Score> topScores;

	public Scores() {
		this.topScores = new ArrayList<>();
	}
	
	public void loadScores() {
		String scores = Preferences.loadValue("scores", "");
		StringTokenizer splitEntries = new StringTokenizer(scores, SCORE_SEPERATOR);
		while (splitEntries.hasMoreElements()) {
			String scoreString = splitEntries.nextToken();
			StringTokenizer splitEntry = new StringTokenizer(scoreString, ENTRY_SEPERATOR);
			String name = splitEntry.nextToken();
			int score = Integer.parseInt(splitEntry.nextToken());
			
			topScores.add(new Score(name, score));
		}
	}
	
	public void saveScores() {
		StringBuilder sb = new StringBuilder();
		Iterator<Score> scoreIterator = topScores.iterator();
		while(scoreIterator.hasNext()) {
			Score score = scoreIterator.next();
			sb.append(score.getName()).append(ENTRY_SEPERATOR).append(score.getScore());
			if (scoreIterator.hasNext()) {
				sb.append(SCORE_SEPERATOR);
			}
		}
		
		Preferences.saveValue("scores", sb.toString());
	}
	
	public CharSequence[] asCharSequence() {
		CharSequence[] scoresArray = new CharSequence[topScores.size()];
		for (int i=0; i<topScores.size(); ++i) {
			Score score = topScores.get(i);
			scoresArray[i] = score.getScore() + " " + score.getName(); 
		}
		
		return scoresArray;
	}
	
	public boolean isTopScore(final int score) {
		if (score == 0) {
			return false;
		}
		if (topScores.size() < 5) {
			return true;
		}
		
		return (topScores.get(topScores.size()-1).getScore() < score);
	}
	
	public void addScore(final String name, final int score) {
		if (isTopScore(score)) {
			topScores.add(new Score(name, score));
			Collections.sort(topScores, new Comparator<Score>() {
				@Override
				public int compare(Score lhs, Score rhs) {
					int scoreOne = lhs.getScore();
					int scoreTwo = rhs.getScore();
					
					if (scoreOne < scoreTwo) {
						return 1;
					}
					if (scoreOne == scoreTwo) {
						return 0;
					}
					
					return -1;
				}
			});
			if (topScores.size() > 5) {
				topScores.remove(topScores.size()-1);
			}
		}
		
		saveScores();
	}
}
