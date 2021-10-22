package cs451.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
    It reads the config file and the number of messages to be sent that is a common field among all the configurations.
 */
public abstract class AbstractConfig {
    private final File configFile;
    private static final String SPACES_REGEX = "\\s+";

    protected List<String[]> lines;

    public AbstractConfig(String filePath) {
        this.configFile = new File(filePath);
        this.lines = new ArrayList<>();

        init();
    }

    private void init() {
        try(BufferedReader br = new BufferedReader(new FileReader(this.configFile))) {
            br.lines().forEach(line -> {
                if (!line.isBlank()) {
                    lines.add(line.split(SPACES_REGEX));
                }
            });
        } catch (IOException e) {
            System.err.println("Problem with the config file!");
        }
    }

    public int getNumberOfMessages() {
        return Integer.parseInt(lines.get(0)[0]);
    }
}
