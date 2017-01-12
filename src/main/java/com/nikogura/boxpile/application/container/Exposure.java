package com.nikogura.boxpile.application.container;

import java.io.Serializable;

/**
 * @author nik.ogura@gmail.com
 *
 * An Exposed Port on a Container
 */
public class Exposure implements Serializable {
    Integer port;

    public Integer getPort() {
        return port;
    }

    public Exposure setPort(Integer port) {
        this.port = port;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Exposure exposure = (Exposure) o;

        return !(port != null ? !port.equals(exposure.port) : exposure.port != null);

    }

    @Override
    public int hashCode() {
        return port != null ? port.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Exposure{" +
                "port=" + port +
                '}';
    }
}
