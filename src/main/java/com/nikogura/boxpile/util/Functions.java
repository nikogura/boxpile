package com.nikogura.boxpile.util;


import com.nikogura.boxpile.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nikogura on 10/1/15.
 */
public class Functions {
    private static Logger logger = LoggerFactory.getLogger(Functions.class);

    public static int addFactorial(int i) {
        if (i - 1 > 0) {
            return i + addFactorial(i - 1);
        } else {
            return i;
        }

    }

    public static boolean imageAvailable(final String imageName) {
        logger.debug("Looking for '" + imageName + "'");

        if (imageName != null) {
            ShellCommand cmd = new ShellCommand()
                    .setWorkDir(System.getProperty("java.io.tmpdir"))
                    .setArgs(new ArrayList<String>() {{
                        add("docker");
                        add("images");
                    }});

            try {
                cmd.exec();

                for (String line : cmd.getStdOutAsList()) {
                    String[] parts = line.split("\\s+");

                    if (parts != null) {
                        if (parts.length > 0) {
                            if (parts[0] != null) {
                                if (parts[0].matches(imageName)) {
                                    return true;
                                }
                            }
                        }
                    }
                }

                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        return false;
    }

    public static boolean imagesAvailable(Application app) {
        Boolean result = true;
        for (String imageName : app.getRequiredImages()) {
            if (!imageAvailable(imageName)) {
                return false;
            }
        }
        return result;
    }

    public static String dockerAddress(String repo, String image) {
        return dockerAddress(repo, image, null);
    }

    public static String dockerAddress(String repo, String image, String version) {
        if (repo == null) {
            if (version == null) {
                return image;
            } else {
                return image + ":" + version;
            }

        } else {
            if (version == null) {
                return repo + "/" + image;
            } else {
                return repo + "/" + image + ":" + version;
            }
        }
    }

    public static boolean dockerAvailable() {
        List<String> args = new ArrayList<String>() {{
            add("docker");
            add("ps");
        }};

        ShellCommand checkDocker = new ShellCommand()
                .setWorkDir("/tmp")
                .setArgs(args);

        try {
            checkDocker.exec();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static String readFileIntoString(URL url) throws IOException {
        return readFileIntoString(url.getPath());
    }

    public static String readFileIntoString(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }

    public static String getTemplatesPath() {
        return "com/nikogura/boxpile/templates/";
    }

    public static String getFilePath() {
        return "com/nikogura/boxpile/files/";
    }
}
