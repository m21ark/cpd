package game.protocols;

public enum GuessErgo {
    PLAYED, // player already played
    NOT_PLAYED, // player not played yet
    WINNING_MOVE, // player made a winning move
    ALREADY_LEFT_GAME, // player already left game
}
