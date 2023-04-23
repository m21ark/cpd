package game.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig implements Configurations {
    public static GameConfig instance;
    private final Properties properties;
    File config = new File("src/resources/config.properties"); // TODO: VER ISTO
    private boolean testMode = false;

    public GameConfig(boolean testMode) {
        // load configurations
        this.testMode = testMode;
        properties = new Properties();
        InputStream inputStream = null;
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

    public int getRankDelta() {
        return Integer.parseInt(properties.getProperty("baseRankDelta"));
    }

    public int getMaxRankDelta() {
        return Integer.parseInt(properties.getProperty("maxRankDelta"));
    }
}
