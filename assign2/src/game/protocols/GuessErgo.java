package game.protocols;

public enum GuessErgo {
    PLAYED, // player already played
    NOT_PLAYED, // player not played yet
    WINNING_MOVE, // player made a winning move
    LEFT_GAME, // TODO: QUAL A DIFERENCA DESTE PARA O DE BAIXO
    ALREADY_LEFT_GAME, // player already left game
}
