package game.server.schedulers;

import game.config.GameConfig;
import game.utils.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record ScheduledSerializer<T extends Serializable>(String filename, T objectToSerialize,
                                                          ScheduledExecutorService scheduler) {

    public void start() {
        long delta = GameConfig.getInstance().getServerCacheInterval();
        if (delta == 0) {
            Logger.warning("Server cache interval is not set. Server cache will not be saved.");
            return;
        }
        scheduler.scheduleAtFixedRate(this::serializeObject, 0, delta, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void serializeObject() {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(objectToSerialize);
            out.close();
            fileOut.close();
            Logger.warning("Serialized object saved to " + filename);
        } catch (IOException ignored) {
            Logger.error("Error serializing object to " + filename);
        }
    }
}
