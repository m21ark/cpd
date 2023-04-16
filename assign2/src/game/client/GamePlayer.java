package game.client;

import java.io.Serializable;

public class GamePlayer implements Serializable {
    private final String name;
    private int rank;
    private int score; // score in this game

    public GamePlayer(String name, int score) {
        this.name = name;
        this.rank = 0;
        this.score = score;
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
