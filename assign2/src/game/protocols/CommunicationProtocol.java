package game.protocols;

public enum CommunicationProtocol {
    NEW_GAME,
    PLAY,
    Guess,
    QUIT,
    ERROR;

    @Override
    public String toString() {
        return this.name() + "\n";
    }
}
