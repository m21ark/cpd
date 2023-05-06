package game.protocols;


import game.server.GameModel;

public class TokenState implements java.io.Serializable {
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

    public void setModel(GameModel model) {
        this.model = model;
    }

    public TokenStateEnum getState() {
        return state;
    }

    public void setState(TokenStateEnum state) {
        this.state = state;
    }

    public enum TokenStateEnum {
        PLAYING, // player is playing
        NOT_PLAYING, // player is not playing
        QUEUED,  // player is queued
        PLAYGROUND,  // TODO: player is in playground (not implemented)
    }
}