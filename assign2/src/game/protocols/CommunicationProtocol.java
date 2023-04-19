package game.protocols;

public enum CommunicationProtocol {
    PLAY, GUESS, QUIT, ERROR, GAME_WAIT, GAME_STARTED, QUEUE_UPDATE, GAME_END;

    @Override
    public String toString() {
        return this.name() + "\n";
    }
}
