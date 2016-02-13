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

public class Scores {
	private static final String SCORE_SEPERATOR = "~~";
	private static final String ENTRY_SEPERATOR = "::";
	private List<Score> topScores;
	private String valueName = "scores";

	public Scores(int numPlayers) {
		this.topScores = new ArrayList<>();

		if (numPlayers > 1) {
			valueName = "scores" + numPlayers;
		}
	}
	
	public void loadScores() {

		String scores = Preferences.loadValue(valueName, "");
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

		Preferences.saveValue(valueName,sb.toString());
	}
	
	public CharSequence[] asCharSequence() {
		CharSequence[] scoresArray = new CharSequence[topScores.size()];
		for (int i=0; i<topScores.size(); ++i) {
			Score score = topScores.get(i);
			scoresArray[i] = score.getName() + " " + score.getScore();
		}
		
		return scoresArray;
	}

	public int getBestScore () {
		if (topScores.size() != 0) {
			Score bestScore = topScores.get(0);
			return bestScore.getScore();
		}

		return 0;
	}
	
	public boolean isTopScore(final int score) {
		if (score == 0) {
			return false;
		}

		if (topScores.size() < 10) {
			return true;
		}
		
		return (topScores.get(topScores.size()-1).getScore() < score);
	}

	public int getRank (int currentScore) {
		for (int i=1; i<=topScores.size(); ++i) {
			Score score = topScores.get(i-1);
			if (score.getScore() == currentScore){
				return i;
			}
		}
		return 0;
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
			if (topScores.size() > 10) {
				topScores.remove(topScores.size()-1);
			}
		}
		
		saveScores();
	}
}
