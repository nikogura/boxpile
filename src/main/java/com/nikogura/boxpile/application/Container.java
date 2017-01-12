package com.nikogura.boxpile.application;

import com.nikogura.boxpile.application.container.Exposure;
import com.nikogura.boxpile.application.container.Link;
import com.nikogura.boxpile.application.container.PortMap;
import com.nikogura.boxpile.application.container.Volume;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik.ogura@gmail.com
 * A class representing an individual container
 */
public class Container implements Serializable {
    String name;
    String image;
    String baseImage;
    String role;
    String group;
    String runCommand;

    List<Link> links = new ArrayList<Link>();
    List<PortMap> ports = new ArrayList<PortMap>();
    List<Exposure> exposures = new ArrayList<Exposure>();
    List<Volume> volumes = new ArrayList<Volume>();
    List<Volume> buildVolumes = new ArrayList<Volume>();

    public String getName() {
        return name;
    }

    public Container setName(String name) {
        this.name = name;
        return this;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public Container setBaseImage(String baseImage) {
        this.baseImage = baseImage;
        return this;
    }

    public String getImage() {
        return image;
    }

    public Container setImage(String image) {
        this.image = image;
        return this;
    }

    public List<Link> getLinks() {
        return links;
    }

    public Container setLinks(List<Link> links) {
        this.links = links;
        return this;
    }

    public List<PortMap> getPorts() {
        return ports;
    }

    public Container setPorts(List<PortMap> ports) {
        this.ports = ports;
        return this;
    }

    public List<Exposure> getExposures() {
        return exposures;
    }

    public Container setExposures(List<Exposure> exposures) {
        this.exposures = exposures;
        return this;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public List<Volume> getBuildVolumes() {
        return buildVolumes;
    }

    public Container setBuildVolumes(List<Volume> volumes) {
        this.buildVolumes = volumes;
        return this;
    }

    public String getRole() {
        return role;
    }

    public Container setRole(String role) {
        this.role = role;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public Container setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public Container setRunCommand(String runCommand) {
        this.runCommand = runCommand;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Container container = (Container) o;

        if (name != null ? !name.equals(container.name) : container.name != null) return false;
        if (image != null ? !image.equals(container.image) : container.image != null) return false;
        if (baseImage != null ? !baseImage.equals(container.baseImage) : container.baseImage != null) return false;
        if (role != null ? !role.equals(container.role) : container.role != null) return false;
        if (group != null ? !group.equals(container.group) : container.group != null) return false;
        if (runCommand != null ? !runCommand.equals(container.runCommand) : container.runCommand != null) return false;
        if (links != null ? !links.equals(container.links) : container.links != null) return false;
        if (ports != null ? !ports.equals(container.ports) : container.ports != null) return false;
        if (exposures != null ? !exposures.equals(container.exposures) : container.exposures != null) return false;
        if (buildVolumes != null ? !buildVolumes.equals(container.buildVolumes) : container.buildVolumes != null) return false;
        return !(volumes != null ? !volumes.equals(container.volumes) : container.volumes != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (baseImage != null ? baseImage.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (runCommand != null ? runCommand.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        result = 31 * result + (exposures != null ? exposures.hashCode() : 0);
        result = 31 * result + (volumes != null ? volumes.hashCode() : 0);
        result = 31 * result + (buildVolumes != null ? buildVolumes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Container{" +
                "name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", baseImage='" + baseImage + '\'' +
                ", role='" + role + '\'' +
                ", group='" + group + '\'' +
                ", runCommand='" + runCommand + '\'' +
                ", links=" + links +
                ", ports=" + ports +
                ", exposures=" + exposures +
                ", volumes=" + volumes +
                ", buildVolumes=" + volumes +
                '}';
    }
}

