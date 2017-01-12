package com.nikogura.boxpile;

import com.nikogura.boxpile.application.Container;

import java.io.Serializable;
import java.util.*;

/**
 * Created by nikogura on 9/22/16.
 */
public class Application {

    Integer appIndex= null;
    Environment env;
    String name;
    LinkedHashMap<String, Container> containers = new LinkedHashMap<String, Container>();
    Map<String, String> containerNameMap = new HashMap<String, String>();
    Set<String> containerNames = new HashSet<String>();
    Set<String> requiredImages = new HashSet<String>();
    Set<String> builtImages = new HashSet<String>();

    public Application(String name) {
        this.setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getContainerNameMap() {
        return containerNameMap;
    }

    public Application setContainerNameMap(Map<String, String> containerNameMap) {
        this.containerNameMap = containerNameMap;
        return this;
    }

    public Set<String> getContainerNames() {
        return containerNames;
    }

    public Application setContainerNames(Set<String> containerNames) {
        this.containerNames = containerNames;
        return this;
    }

    public Integer getAppIndex() {
        return appIndex;
    }

    public Application setAppIndex(Integer appIndex) {
        this.appIndex = appIndex;
        return this;
    }

    public Environment getEnv() {
        return env;
    }

    public Application setEnv(Environment env) {
        this.env = env;
        return this;
    }

    public Map<String, Container> getContainers() {
        return containers;
    }

    public Application setContainers(LinkedHashMap<String, Container> containers) {
        this.containers = containers;
        return this;
    }

    public Set<String> getRequiredImages() {
        return requiredImages;
    }

    public Application setRequiredImages(Set<String> requiredImages) {
        this.requiredImages = requiredImages;
        return this;
    }

    public Set<String> getBuiltImages() {
        return builtImages;
    }

    public void setBuiltImages(Set<String> builtImages) {
        this.builtImages = builtImages;
    }

    // not totally boilerplate!
    // the following replaces default implementation to deal with the implementation of equals() as it applies to the the array at the heart of LinkedHashMap
    // if (containers != null ? !(containers.hashCode() == that.containers.hashCode()) : that.containers != null) return false;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Application that = (Application) o;

        if (this.getName() != null ? !this.getName().equals(that.getName()) : that.getName() != null) return false;
        if (appIndex != null ? !appIndex.equals(that.appIndex) : that.appIndex != null) return false;
        if (env != null ? !env.equals(that.env) : that.env != null) return false;
        if (containers != null ? !(containers.hashCode() == that.containers.hashCode()) : that.containers != null) return false;
        if (containerNameMap != null ? !containerNameMap.equals(that.containerNameMap) : that.containerNameMap != null)
            return false;
        if (requiredImages != null ? !(requiredImages.hashCode() == that.requiredImages.hashCode()) : that.requiredImages != null) return false;
        if (builtImages != null ? !(builtImages.hashCode() == that.builtImages.hashCode()) : that.builtImages != null) return false;
        return !(containerNames != null ? !containerNames.equals(that.containerNames) : that.containerNames != null);

    }

    @Override
    public int hashCode() {
        int result = this.getName() != null ? this.getName().hashCode() : 0;
        result = 31 * result + (appIndex != null ? appIndex.hashCode() : 0);
        result = 31 * result + (env != null ? env.hashCode() : 0);
        result = 31 * result + (containers != null ? containers.hashCode() : 0);
        result = 31 * result + (containerNameMap != null ? containerNameMap.hashCode() : 0);
        result = 31 * result + (requiredImages != null ? requiredImages.hashCode() : 0);
        result = 31 * result + (builtImages != null ? builtImages.hashCode() : 0);
        result = 31 * result + (containerNames != null ? containerNames.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Application{" +
                "name='" + this.getName() + '\'' +
                ", appIndex=" + appIndex +
                ", env=" + env +
                ", containers=" + containers +
                ", containerNameMap=" + containerNameMap +
                ", containerNames=" + containerNames +
                ", requiredImages=" + requiredImages +
                ", builtImages=" + builtImages +
                '}';
    }
}
