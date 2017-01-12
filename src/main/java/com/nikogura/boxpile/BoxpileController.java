package com.nikogura.boxpile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;

/**
 * Created by Nik Ogura on 11/22/16.
 */
@SpringBootApplication
public class BoxpileController {
    @Autowired
    Properties props;

}
