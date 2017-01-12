package com.nikogura.boxpile.application.container;

import java.io.Serializable;

/**
 * @author nik.ogura@gmail.com
 *
 * Port Mapping between the Docker Host and a Container
 */
public class PortMap implements Serializable {
    Integer host;
    Integer container;

    public Integer getHost() {
        return host;
    }

    public PortMap setHost(Integer host) {
        this.host = host;
        return this;
    }

    public Integer getContainer() {
        return container;
    }

    public PortMap setContainer(Integer container) {
        this.container = container;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortMap portMap = (PortMap) o;

        if (host != null ? !host.equals(portMap.host) : portMap.host != null) return false;
        return !(container != null ? !container.equals(portMap.container) : portMap.container != null);

    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (container != null ? container.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PortMap{" +
                "host=" + host +
                ", container=" + container +
                '}';
    }
}
