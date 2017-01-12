package com.nikogura.boxpile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Nik Ogura on 1/11/17.
 */
@Configuration
public class Config {
    Logger logger = LoggerFactory.getLogger(Config.class);

    @Bean
    public Properties props() {
        Properties props = new Properties();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        InputStream stream = null;

        try {
            stream = loader.getResourceAsStream("application.properties");
            props.load(stream);
        } catch (FileNotFoundException e) {
            logger.trace("No Private Properties File");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return props;
    }
}
