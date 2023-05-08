package game.server.schedulers;

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
        for (PlayingServer.WrappedPlayerSocket player : playingServer.queueToPlay) {
            player.increaseTolerance();
            Logger.info("Player " + player.getName()
                    + " tolerance increased to " + player.getTolerance());

        }
    }
}
