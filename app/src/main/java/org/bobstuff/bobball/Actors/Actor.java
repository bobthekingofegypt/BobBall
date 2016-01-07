package org.bobstuff.bobball.Actors;

import org.bobstuff.bobball.GameLogic.GameEvent;
import org.bobstuff.bobball.GameLogic.GameManager;

// an actor is someone that controls one or multiple players
// an actors run method is called *roughly* on every gamestep
public abstract class  Actor implements Runnable {
    protected GameManager gameManager;
    protected int[] playerIds;
    /**
     * approx. freq of the calls to the run method  per game cycle
     * 0 means call only once
     */
    public float getExecFreq(){return 0.0f;};

    public Actor(GameManager gameManager, int[] playerIds) {
        this.gameManager = gameManager;
        this.playerIds = playerIds;
    }

    public void newEventCallback(GameEvent ev)
    {
    }
    public abstract void reset();
}
