package game.logic;

import java.net.Socket;
import java.util.List;

public class GameModel {
    private List<Socket> userSockets;

    public GameModel(List<Socket> userSockets) {
        this.userSockets = userSockets;
    }

    public void start() {

    }
}
