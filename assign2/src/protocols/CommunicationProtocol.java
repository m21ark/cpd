package game.protocols;

public enum CommunicationProtocol {

    // ================== Connection Related ==================
    PLAYER_LEFT, // TODO: Qual a diferença entre disconnected, logout, left ? --> left está a ser usado para varias coisas ?
    DISCONNECTED, // Server or client informs that they disconnected. No args

    // ================== Game Waiting ==================
    QUEUE_ADD, // Server informs client that he will be added to queue. No args  // TODO: Client não está à espera disto!
    QUEUE_UPDATE, // TODO: Server informs client that the queue has X players in it, that there are Y games and 1 is ending etc
    PLAYGROUND_UPDATE, // Server notifies players in the playground of how many players are still needed to start game. Args: CurrNumPlayers, NeededNumPlayers
    GAME_STARTED, // Server notifies all players that the game has begun. Args: MaxNumGuesses, NumPlayers, MaxValue

    // ================== Game related communication ==================
    GUESS, // Player sends guess to server. Args: playerGuessValue
    GUESS_TOO_HIGH, // Server response to player's guess. No args
    GUESS_TOO_LOW, // Server response to player's guess. No args
    GUESS_CORRECT, // Server response if player guesses correctly. Args: secretWinningNumber
    GAME_END, // Server message to all players when the game ends. No args
    GAME_RESULT, // Server message to all players telling scores. Args: Points, LeaderboardPosition, NumberOfPlayers, ClosestGuess, secretWinningNumber

    // ==================  Reconnection info send from server --> client ==================
    MENU_CONNECT, // Server informs client that there are no reconnects to be done and can go to menu. No args
    QUEUE_RECONNECT, // Server tells client that he was previously in a queue. No args
    PLAYGROUND_RECONNECT, // Server tells client that he was previously in a game playground. Args: CurrentPlayerCount, NumPlayersForGameToStart
    GAME_RECONNECT // Server tells client that he was previously in the middle of a game. Args: MaxNumGuesses, NumPlayers, MaxGuessValue, NumGuessesLeft, BestGuessYet, BessGuessHint (higher/lower)
    ;

    @Override
    public String toString() {
        return " " + this.name() + " ";
    }
}
