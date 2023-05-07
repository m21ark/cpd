package game.protocols;

public enum CommunicationProtocol {
    PLAY,
    PLAYER_LEFT,
    QUEUE_ADD,
    QUIT,
    ERROR,
    LOGOUT,
    GAME_WAIT,
    GAME_STARTED,
    DISCONNECTED,

    MENU_CONNECT,
    PLAYGROUND_UPDATE,
    QUEUE_UPDATE,

    // ================== Game related communication ==================
    GUESS, // Player sends guess to server. Args: playerGuessValue
    GUESS_TOO_HIGH, // Server response to player's guess. No args
    GUESS_TOO_LOW, // Server response to player's guess. No args
    GUESS_CORRECT, // Server response if player guesses correctly. Args: secretWinningNumber
    GAME_END, // Server message to all players when the game ends. No args
    GAME_RESULT, // Server message to all players telling scores. Args: Points, LeaderboardPosition, NumberOfPlayers, ClosestGuess, secretWinningNumber

    // ==================  Reconnection info send from server --> client ==================
    QUEUE_RECONNECT, // Server tells client that he was previously in a queue
    PLAYGROUND_RECONNECT, // Server tells client that he was previously in a game playground
    GAME_RECONNECT // Server tells client that he was previously in the middle of a game
    ;

    @Override
    public String toString() {
        return " " + this.name() + " ";
    }
}
