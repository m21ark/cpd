package game.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig implements Configurations {
    private final Properties properties;
    private boolean testMode = false;
    File config = new File("assign2/src/resources/config.properties"); // TODO: VER ISTO

    public GameConfig(boolean testMode) throws IOException {
        // load configurations
        this.testMode = testMode;
        properties = new Properties();
        InputStream inputStream = new FileInputStream(config);
        properties.load(inputStream);
    }

    public GameConfig() throws IOException {
        // load configurations
        properties = new Properties();
        InputStream inputStream = new FileInputStream(config);
        properties.load(inputStream);
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
}
