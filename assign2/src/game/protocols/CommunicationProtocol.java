package game.protocols;

public enum CommunicationProtocol {
    PLAY, GUESS, QUIT, ERROR, GAME_WAIT, GAME_STARTED, QUEUE_UPDATE, GAME_END, GUESS_TOO_HIGH, GUESS_TOO_LOW, GUESS_CORRECT, GAME_WON, GAME_LOST;

    @Override
    public String toString() {
        return this.name() + "\n";
    }
}
