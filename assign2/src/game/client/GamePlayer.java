package game.client;

import java.io.Serializable;

public class GamePlayer implements Serializable {
    protected final String name;
    protected int rank;
    protected int score; // score in this game (aka best guess yet)

    public GamePlayer(String name, int rank) {
        this.name = name;
        this.rank = rank;
        this.score = 0;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int score) {
        this.score += score;
    }
}
