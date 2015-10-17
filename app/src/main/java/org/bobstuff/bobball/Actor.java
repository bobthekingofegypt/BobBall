package org.bobstuff.bobball;

// an actor is someone that controls one or multiple players
// an actors run method is called *roughly* on every gamestep
public abstract class Actor implements Runnable {
    protected GameManager gameManager;
    protected int[] playerIds;

    public Actor(GameManager gameManager, int[] playerIds) {
        this.gameManager = gameManager;
        this.playerIds = playerIds;
    }

    public abstract void reset();
}
