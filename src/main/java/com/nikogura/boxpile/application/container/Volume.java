package com.nikogura.boxpile.application.container;

import java.io.Serializable;

/**
 * Created by nikogura on 9/4/15.
 */
public class Volume implements Serializable {
    private String sourcePath;
    private String buildSourcePath;
    private String containerPath;

    public String getSourcePath() {
        return sourcePath;
    }

    public Volume setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    public String getBuildSourcePath() {
        return buildSourcePath;
    }

    public Volume setBuildSourcePath(String buildSourcePath) {
        this.buildSourcePath = buildSourcePath;
        return this;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public Volume setContainerPath(String containerPath) {
        this.containerPath = containerPath;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Volume volume = (Volume) o;

        if (sourcePath != null ? !sourcePath.equals(volume.sourcePath) : volume.sourcePath != null) return false;
        if (buildSourcePath != null ? !buildSourcePath.equals(volume.buildSourcePath) : volume.buildSourcePath != null) return false;
        return !(containerPath != null ? !containerPath.equals(volume.containerPath) : volume.containerPath != null);

    }

    @Override
    public int hashCode() {
        int result = sourcePath != null ? sourcePath.hashCode() : 0;
        result = buildSourcePath != null ? buildSourcePath.hashCode() : 0;
        result = 31 * result + (containerPath != null ? containerPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Volume{" +
                "sourcePath='" + sourcePath + '\'' +
                "buildSourcePath='" + buildSourcePath + '\'' +
                ", containerPath='" + containerPath + '\'' +
                '}';
    }
}
