package game.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig implements Configurations{
    private final Properties properties;

    public GameConfig() throws IOException {
        properties = new Properties();
        File file = new File("assign2/src/resources/config.properties"); // TODO: VER ISTO
        InputStream inputStream = new FileInputStream(file);
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
}
