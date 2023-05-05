package game.protocols;


import game.logic.GameModel;

public class TokenState implements java.io.Serializable{
    public enum TokenStateEnum {
        PLAYING, // player is playing
        NOT_PLAYING, // player is not playing
        QUEUED, // player is queued
    }

    private TokenStateEnum state;
    private GameModel model = null;

    public TokenState() {
        this.state = TokenStateEnum.NOT_PLAYING;
    }

    public TokenState(GameModel model) {
        this.state = TokenStateEnum.PLAYING;
        this.model = model;
    }

    public TokenState(GameModel model, TokenStateEnum state) {
        this.state = state;
        this.model = model;
    }


    public GameModel getModel() {
        return model;
    }

    public TokenStateEnum getState() {
        return state;
    }

    public void setState(TokenStateEnum state) {
        this.state = state;
    }

    public void setModel(GameModel model) {
        this.model = model;
    }
}