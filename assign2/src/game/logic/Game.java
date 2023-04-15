package game.logic;

import java.net.Socket;
import java.util.List;

public class Game {
    private List<Socket> userSockets;

    public Game(List<Socket> userSockets) {
        this.userSockets = userSockets;
    }

    public void start() {

    }
}
