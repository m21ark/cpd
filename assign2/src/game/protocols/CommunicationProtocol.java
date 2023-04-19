package game.protocols;

public enum CommunicationProtocol {
    NEW_GAME, GAME_STARTING, PLAY, GUESS, QUIT, ERROR, GAME_WAIT, GAME_STARTED;

    @Override
    public String toString() {
        return this.name() + "\n";
    }
}
