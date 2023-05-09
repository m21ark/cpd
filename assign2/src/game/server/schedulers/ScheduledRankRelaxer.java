package game.server.schedulers;

import game.server.GameModel;
import game.server.PlayingServer;
import game.utils.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledRankRelaxer {
    ScheduledExecutorService scheduler;
    PlayingServer playingServer;

    public ScheduledRankRelaxer(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        playingServer = PlayingServer.getInstance();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::relaxRanks, 0, 15, TimeUnit.SECONDS);
    }

    private void relaxRanks() {
        for (GameModel game : playingServer.games) {
            int gameSize = game.getCurrentPlayers();
            boolean hasEnoughPlayer = game.playgroundUpdate(false); // check if a ranked player waiting can enter this updated game
            if (hasEnoughPlayer) {
                Logger.info("Game started");
                PlayingServer.getInstance().games.updateHeap(game);
                PlayingServer.executorGameService.submit(game);
            } else if (gameSize != game.getCurrentPlayers()) {
                Logger.info("Relaxing tolerance");
                PlayingServer.getInstance().games.updateHeap(game);
            }
        }
    }
}
