package com.nikogura.boxpile;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nikogura on 9/22/16.
 */
public class Environment {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Environment(String name) {
        this.setName(name);
    }

}
