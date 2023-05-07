package game.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig implements Configurations, java.io.Serializable {
    public static GameConfig instance;
    private final Properties properties;
    File config;
    private boolean testMode;

    public GameConfig(boolean testMode) {
        String workingDir = System.getProperty("user.dir");
        if (workingDir.contains("assign2")) config = new File("src/resources/config.properties");
        else config = new File("assign2/src/resources/config.properties");
        // load configurations
        this.testMode = testMode;
        properties = new Properties();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(config);
            properties.load(inputStream);
            instance = this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig(false);
        }
        return instance;
    }

    @Override
    public String getAddress() {
        return properties.getProperty("address");
    }

    @Override
    public int getPort() {
        return Integer.parseInt(properties.getProperty("port"));
    }

    public int getRMIReg() {
        return Integer.parseInt(properties.getProperty("rmiReg"));
    }

    @Override
    public boolean isTestMode() {
        return testMode;
    }

    @Override
    public String getMode() {
        return properties.getProperty("mode");
    }

    public int getBaseRankDelta() {
        return Integer.parseInt(properties.getProperty("baseRankDelta"));
    }

    public int getMaxRankDelta() {
        return Integer.parseInt(properties.getProperty("maxRankDelta"));
    }

    public long getGameTimeout() {
        return Long.parseLong(properties.getProperty("gameTimeoutTime"));
    }

    public int getMaxNrGuess() {
        return Integer.parseInt(properties.getProperty("maxNrGuesses"));
    }

    public int getMaxGuess() {
        return Integer.parseInt(properties.getProperty("maxGuessValue"));
    }

    public int getNrMaxPlayers() {
        return Integer.parseInt(properties.getProperty("nrMaxPlayersInGame"));
    }

    public long getTokenLifeSpan() {
        return Integer.parseInt(properties.getProperty("tokenLifeSpanSec"));
    }

    public long getServerCacheInterval() {
        return Integer.parseInt(properties.getProperty("serverCacheInterval"));
    }

    public int getGamePoolSize() {
        return Integer.parseInt(properties.getProperty("gamePoolSize"));
    }
}
