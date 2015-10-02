package org.bobstuff.bobball;

// an actor is someone that controls one or multiple players
// an actor runs in its own thread/asynchronously
public abstract class Actor extends Thread {
    protected GameManager gameManager;
    protected int[] playerIds;

    public Actor(GameManager gameManager, int[] playerIds) {
        this.gameManager = gameManager;
        this.playerIds = playerIds;
    }

    public abstract void reset();
}
