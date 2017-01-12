package com.nikogura.boxpile.application.container;

import java.io.Serializable;

/**
 * @author nik.ogura@gmail.com
 *
 * Linke between 2 containers
 */
public class Link implements Serializable{
    String containerName;
    String alias;
    String role;

    public String getContainerName() {
        return containerName;
    }

    public Link setContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public Link setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getRole() {
        return role;
    }

    public Link setRole(String role) {
        this.role = role;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        if (containerName != null ? !containerName.equals(link.containerName) : link.containerName != null)
            return false;
        if (alias != null ? !alias.equals(link.alias) : link.alias != null) return false;
        return !(role != null ? !role.equals(link.role) : link.role != null);

    }

    @Override
    public int hashCode() {
        int result = containerName != null ? containerName.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Link{" +
                "containerName='" + containerName + '\'' +
                ", alias='" + alias + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
