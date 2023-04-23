package game.protocols;

public enum CommunicationProtocol {
    PLAY, GUESS, QUIT, ERROR, GAME_WAIT, GAME_STARTED, QUEUE_UPDATE, GAME_END, GUESS_TOO_HIGH, GUESS_TOO_LOW, GAME_WON;

    @Override
    public String toString() {
        return this.name() + "\n";
    }
}
