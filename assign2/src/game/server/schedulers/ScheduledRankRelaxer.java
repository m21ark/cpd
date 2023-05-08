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
        scheduler.scheduleAtFixedRate(this::relaxRanks,
                0, 15, TimeUnit.SECONDS);
    }

    private void relaxRanks() {
        Logger.info("Relaxing ranks");
        for (GameModel game : playingServer.games) {
            boolean hasEnoughPlayer =  game.playgroundUpdate(); // check if a ranked player waiting can enter this updated game
            if (hasEnoughPlayer) {
                Logger.info("Game started");
                PlayingServer.executorGameService.submit(game);
            } else {
                playingServer.notifyPlaygroundUpdate(game);
            }
        }
    }
}
