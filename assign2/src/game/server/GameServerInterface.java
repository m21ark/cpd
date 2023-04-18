package game.server;

import game.client.GamePlayer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServerInterface extends Remote {
    void queueGame(GamePlayer client, String token) throws RemoteException;

}
