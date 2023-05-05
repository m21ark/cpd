package game.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledSerializer<T extends Serializable> {
    private final String filename;
    private final T objectToSerialize;
    private final ScheduledExecutorService scheduler;

    public ScheduledSerializer(String filename, T objectToSerialize) {
        this.filename = filename;
        this.objectToSerialize = objectToSerialize;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::serializeObject, 0, 1, TimeUnit.MINUTES);
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
            System.out.println("Serialized object saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
