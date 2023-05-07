package game.protocols;


import game.server.GameModel;

public class TokenState implements java.io.Serializable {
    private final TokenStateEnum state;
    private GameModel model = null;

    public TokenState() {
        this.state = TokenStateEnum.MENU;
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

    @Override
    public String toString() {
        return "TokenState{" + "state=" + state + ", model=" + model + '}';
    }

    public enum TokenStateEnum {
        MENU, // player is in the menu after login
        QUEUED,  // player is queued
        PLAYGROUND,  // player is in the playground
        PLAYING // player is playing
    }
}