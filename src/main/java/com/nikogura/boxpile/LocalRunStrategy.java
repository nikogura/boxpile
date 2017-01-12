package com.nikogura.boxpile;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikogura.boxpile.application.Container;
import com.nikogura.boxpile.application.container.Link;
import com.nikogura.boxpile.application.container.PortMap;
import com.nikogura.boxpile.application.container.Volume;
import com.nikogura.boxpile.exception.DockerRunException;
import com.nikogura.boxpile.exception.UninitializedComponentException;
import com.nikogura.boxpile.util.Functions;
import com.nikogura.boxpile.util.ShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nik.ogura@gmail.com
 * Takes an Container and executes the requisite run commands to run and link the various containers together
 */
public class LocalRunStrategy {
    // TODO Provide for app restart in DockerLocalRunProvider
    File contextDir;

    Map<String, Object> containerInfoById = new HashMap<String, Object>();
    Map<String, Object> containerInfoByName = new HashMap<String, Object>();
    Boolean quiet = false;
    String relativeDirPattern = "^\\.\\.?\\/(.*)";

    private static Logger logger = LoggerFactory.getLogger(LocalRunStrategy.class);

    public enum ExistingContainerRunAction {
        START,
        STOP,
        RM,
        KILL,
    }

    public enum RefreshType {
        ALL,
        RUNNING,
    }

    public LocalRunStrategy() {
        String tmpRoot = System.getProperty("java.io.tmpdir");
        String tmpDir = "boxpile";
        contextDir = new File(tmpRoot + File.separator + tmpDir);
        contextDir.mkdir();

        // create subdirs
        for (String dir : new ArrayList<String>() {{
            add("private");
            add("hiera");
        }}) {
            File dirFile = new File(contextDir.getPath() + File.separator + dir);
            dirFile.mkdir();
        }

    }

    public File getContextDir() {
        return contextDir;
    }

    public LocalRunStrategy setContextDir(File contextDir) {
        this.contextDir = contextDir;
        return this;
    }

    public Boolean getQuiet() {
        return quiet;
    }

    public LocalRunStrategy setQuiet(Boolean quiet) {
        this.quiet = quiet;
        return this;
    }

    public String getName() {
        return null;
    }

    public Boolean authRequired() {
        return false;
    }

    public Boolean netRequired() {
        return false;
    }

    public void run(Application app) throws DockerRunException, UninitializedComponentException {
        app.setAppIndex(getNextAvailableAppIndexFor(app));

        runContainers(app);
    }

    public void stop(Application app) throws DockerRunException {

        stopContainers(app.getContainerNames());
    }

    public void kill(Application app) throws DockerRunException {

        logger.debug("Killing App: "+ app.getName());
        logger.debug("Containers: "+ app.getContainerNames());
        killContainers(app.getContainerNames());
        logger.debug("Done with Kill");
    }

    public void rm(Application app) throws DockerRunException {

        logger.debug("Removing Containers for App: "+ app.getName());
        logger.debug("Containers: "+ app.getContainerNames());
        rmContainers(app.getContainerNames());
        logger.debug("Done with rm");
    }

    /*
    public boolean imageAvailable(final String imageName) {
        logger.debug("Looking for '" + imageName + "'");

        ShellCommand cmd = new ShellCommand()
                .setWorkDir(contextDir.getAbsoluteFile())
                .setArgs(new ArrayList<String>() {{
                    add("docker");
                    add("images");
                }});

        try {
            cmd.exec();

            for (String line : cmd.getStdOutAsList()) {
                String[] parts = line.split("\\s+");

                if (parts[0].matches(imageName)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e)  {
            e.printStackTrace();
            return false;
        }
    }

    public boolean imagesAvailable(Application app) {
        Boolean result = true;
        for (String imageName : app.getRequiredImages()) {
            if (!imageAvailable(imageName)) {
                return false;
            }
        }
        return result;
    }
    */

    public ShellCommand createDockerRunCommand(Application app, Container c, String containerName) {
        logger.debug("Creating Docker Run Command");
        List<String> args = new ArrayList<String>() {{
            add("docker");
        }};

        args.add("run");
        args.add("-d");

        if (!c.getPorts().isEmpty()) {
            for (PortMap p : c.getPorts()) {
                args.add("-p");
                args.add(p.getHost()+":"+p.getContainer());
            }

        } else {
            logger.debug("No Ports");
        }

        logger.debug("Container Name: "+ c.getName());
        logger.debug("Volumes: "+ c.getVolumes());

        if (!c.getVolumes().isEmpty()) {
            logger.debug("Processing volumes");
            for (Volume vol : c.getVolumes()) {
                args.add("-v");

                Matcher m = Pattern.compile(relativeDirPattern).matcher(vol.getSourcePath());

                // if we have a relative path of any sort, we need to get the actual dir this baby was run from
                if (m.find()) {
                    logger.debug("We have a relative path! ");
                    String wdName = System.getProperty("user.dir");

                    File parentFolder = new File(wdName);

                    String rawSourcePath = vol.getSourcePath();

                    File sourcePath;

                    // pure cosmetic block
                    if (rawSourcePath.matches("\\.\\.\\/.*")) {                                            // let it pass if the path starts ../
                        sourcePath = new File(parentFolder, rawSourcePath);

                    } else if (rawSourcePath.matches("\\.\\/.*")) {
                        sourcePath = new File(parentFolder, rawSourcePath.replaceFirst("\\.\\/", ""));     // remove leading ./ if present

                    } else {
                        sourcePath = new File(parentFolder, rawSourcePath);                                // pass it unmodified (same as first branch)
                    }

                    // end cosmetic block  Without it paths like /project/dsBranch/Hawkeye/./target/HawkeyeHome get used.  They're harmless, but ugly

                    String newPath = sourcePath.getAbsolutePath();

                    args.add(newPath + ":" + vol.getContainerPath());
                } else {
                    args.add(vol.getSourcePath() + ":" + vol.getContainerPath());
                }
            }
        } else {
            logger.debug("No Volumes");
        }

        if (!c.getLinks().isEmpty()) {
            for (Link lnk : c.getLinks()) {
                args.add("--link");
                if (app.getContainerNameMap().get(lnk.getContainerName()) != null ) {
                    args.add(app.getContainerNameMap().get(lnk.getContainerName())+":"+lnk.getAlias());

                } else {
                    args.add(lnk.getContainerName()+":"+lnk.getAlias());

                }
            }

        } else {
            logger.debug("No Links");
        }

        args.add("--name");

        args.add(containerName);

        // image name
        if (c.getImage() != null) {  // containers from images just use the image
            args.add(c.getImage());

        } else {  // otherwise, it will be an image we built here and will be <name>-<env>
            args.add(c.getName() + "-" + app.getEnv().getName());
        }

        ShellCommand cmd = new ShellCommand()
                .setWorkDir(contextDir.getAbsoluteFile())
                .setArgs(args);

        if (!quiet) {
            cmd.setPrintStdOut(true);
            cmd.setPrintStdErr(true);
        }

        return cmd;

    }

    public ShellCommand createDockerStopCommand(String containerName) {
        return createExistingDockerCommand(containerName, ExistingContainerRunAction.STOP);
    }

    public ShellCommand createDockerKillCommand(String containerName) {
        return createExistingDockerCommand(containerName, ExistingContainerRunAction.KILL);
    }

    public ShellCommand createDockerStartCommand(String containerName) {
        return createExistingDockerCommand(containerName, ExistingContainerRunAction.START);
    }

    public ShellCommand createDockerRmCommand(String containerName) {
        return createExistingDockerCommand(containerName, ExistingContainerRunAction.RM);
    }

    public ShellCommand createExistingDockerCommand(String containerName, ExistingContainerRunAction action) {

        List<String> args = new ArrayList<String>() {{
            add("docker");
        }};

        switch (action) {
            case STOP:
                args.add("stop");

                break;

            case START:
                args.add("start");

                break;

            case KILL:
                args.add("kill");
                break;

            case RM:
                args.add("rm");

                break;

        }

        args.add(containerName);

        return new ShellCommand()
                .setWorkDir(contextDir.getAbsoluteFile())
                .setArgs(args);

    }

    public Set<String> runContainers(Application app) throws DockerRunException, UninitializedComponentException {
        // can't just walk the list, as they have to come up in reverse dependency order

        Set<String> containerNames = new HashSet<String>();
        List<Container> cList = new ArrayList<Container>();

        for (String name : app.getContainers().keySet()) {                        // make our own copy to use as a stack
            cList.add(app.getContainers().get(name));
        }

        int limit = Functions.addFactorial(cList.size());                         // the max number of repetitions we're willing to wait

        while (!cList.isEmpty() && limit > -1) {
            Container c = cList.remove(0);                                        // shift in javaspeak.  pull the first element off the list

            limit --;

            if (allDepsRunning(app, c)) {                                         // all this container's dependences are currently running
                String imageName;

                logger.debug("figuring out image name");
                if (c.getImage() != null) {
                    logger.debug("there's an image, use it");
                    imageName = c.getImage();
                } else {
                    logger.debug("no image, so it's continer name + env ");
                    imageName = c.getName() + "-" + app.getEnv().getName();
                }

                logger.debug("Looking for "+ imageName);

                if (Functions.imageAvailable(imageName)) {                                // and assuming I have an image built
                    try {
                        String containerName = runContainer(app, c);             // run it
                        containerNames.add(containerName);
                    } catch (Exception e) {
                        throw new DockerRunException(e);
                    }

                } else {
                    throw new DockerRunException("No Image: " + imageName + " aborting.");
                }

            } else {
                cList.add(c);                                        // stick it back on the pile
            }
        }

        if (limit == -1) {
            logger.debug("Remaining Containers: "+ cList);
            throw new DockerRunException("Container Dependencies can't be met:  ");
        }

        return containerNames;
    }

    public void stopContainers(Set<String> names) throws DockerRunException {
        for (String name : names) {
            try {
                stopContainer(name);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DockerRunException(e);
            }
        }
    }

    public void killContainers(Set<String> names) throws DockerRunException {
        logger.debug("Killing Containers");
        for (String name : names) {
            try {
                logger.debug("Killing: "+ name);
                killContainer(name);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DockerRunException(e);
            }
        }
    }

    /*
        Removes containers from the docker host, as well as from the list of container names
     */
    public void rmContainers(Set<String> names) throws DockerRunException {
        List<String> removeNames = new ArrayList<String>();
        for (String name : names) {
            try {
                rmContainer(name);
                removeNames.add(name);
            } catch (Exception e) {
                throw new DockerRunException(e);
            }
        }

        for (String name : removeNames) {
            names.remove(name);
        }
    }

    public Boolean containersRunning(Application app) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.RUNNING);                                     // make sure we're up to date with running containers

        Boolean allUp = true;

        for (Container c : app.getContainers().values()) {
            if (!containerRunning(app, c)) {
                allUp = false;
                break;
            }
        }

        return allUp;
    }

    public Boolean containerRunning(Application app, Container c) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.RUNNING);                                     // make sure we're up to date with running containers

        String prefix = getContainerPrefix(app, c);

        Pattern dependencyPattern = Pattern.compile("^/" + prefix);

        Boolean containerUp = false;

        logger.debug("Looking for: "+ prefix);

        logger.debug(containerInfoByName.keySet().size() + " containers on host");

        for (String name : containerInfoByName.keySet()) {
            Matcher m = dependencyPattern.matcher(name);

            logger.debug("checking "+ name);

            if (m.find()) {
                containerUp = true;
                break;
            }
        }

        return containerUp;
    }

    public Boolean containersExist(Application app) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.ALL);                                     // make sure we're up to date with running containers

        Boolean allUp = true;

        for (Container c : app.getContainers().values()) {
            if (!containerRunning(app, c)) {
                allUp = false;
                break;
            }
        }

        return allUp;
    }

    public Boolean containerExists(Application app, Container c) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.ALL);                                     // make sure we're up to date with running containers

        String prefix = getContainerPrefix(app, c);

        Pattern dependencyPattern = Pattern.compile("^/" + prefix);

        Boolean containerUp = false;

        logger.debug("Looking for: "+ prefix);

        logger.debug(containerInfoByName.keySet().size() + " containers on host");

        for (String name : containerInfoByName.keySet()) {
            Matcher m = dependencyPattern.matcher(name);

            logger.debug("checking "+ name);

            if (m.find()) {
                containerUp = true;
                break;
            }
        }

        return containerUp;
    }

    public String runContainer(Application app, Container c) throws InterruptedException, IOException, UninitializedComponentException {

        if (app.getAppIndex() == null) {
            app.setAppIndex(getNextAvailableAppIndexFor(app));
        }

        logger.debug("Creating Shell Command for " + c.getName());
        String containerName = getNameForNewContainer(app, c);

        app.getContainerNameMap().put(c.getName(), containerName);
        app.getContainerNames().add(containerName);

        ShellCommand dockerRunCmd = createDockerRunCommand(app, c, containerName);                // gen docker run command

        if (!quiet) {
                System.out.println("Docker Run Command: (" + containerName + ") "+ dockerRunCmd.toString());
        }

        logger.debug("Running Container: " + c.getName());
        dockerRunCmd.exec();                                                                 // run shell command

        return containerName;
    }

    public void stopContainer(String containerName) throws InterruptedException, IOException  {

        ShellCommand dockerStopCmd = createDockerStopCommand(containerName);                 // gen docker run command

        logger.debug("Stopping Container: " + containerName);

        try {
            dockerStopCmd.exec();                                                                // run shell command
        } catch (IOException e) {
            logger.debug("Exception on stop command: "+ e.getMessage());

            // an exception that has 'is not running' in it's message is fine.  That just means we tried to kill a container that isn't running.
            Pattern notRunning = Pattern.compile(".*is not running.*");

            if (!notRunning.matcher(e.getMessage()).find()) {
                throw e;

            }
        }

    }

    public void killContainer(String containerName) throws InterruptedException, IOException  {

        ShellCommand dockerKillCmd = createDockerKillCommand(containerName);                 // gen docker run command

        logger.debug("Killing Container: " + containerName);

        try {
            dockerKillCmd.exec();                                                                // run shell command
        } catch (IOException e) {
            logger.debug("Exception on kill command: "+ e.getMessage());

            // an exception that has 'is not running' in it's message is fine.  That just means we tried to kill a container that isn't running.
            Pattern notRunning = Pattern.compile(".*is not running.*");

            if (!notRunning.matcher(e.getMessage()).find()) {
                throw e;

            }
        }

        logger.debug("Kill Command Run");

    }

    public void rmContainer(String containerName) throws InterruptedException, IOException  {
        ShellCommand dockerRmCmd = createDockerRmCommand(containerName);                    // gen docker run command

        logger.debug("Removing Container: " + containerName);
        dockerRmCmd.exec();                                                                 // run shell command

    }

    public boolean allDepsRunning(Application app, Container c) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.RUNNING);                                     // make sure we're up to date with running containers

        if (c.getLinks().isEmpty()) {                               // if this container has no dependencies, then we're good.  Return early.
            return true;
        } else {
            /*
                The app has a link, but the container would look like: <appName>_<appIndex>_<role>_<container index> .

                Not sure how scalable this will be long term, but for now, we'll assume if 1 container of role X is up, then we're good to go.

             */

            Boolean allUp = true;

            for (Link link : c.getLinks()) {
                String cName = getContainerPrefix(app, link.getContainerName());
                Pattern dependencyPattern = Pattern.compile("^/" + cName);

                Boolean linkUp = false;

                logger.debug("Looking for Dependency: "+ cName);

                for (String name : containerInfoByName.keySet()) {
                    Matcher m = dependencyPattern.matcher(name);

                    if (m.find()) {
                        linkUp = true;
                        break;
                    }
                }

               if (!linkUp) {
                   allUp = false;                                   // any linkUp that's false makes allUp false, and that's it.
                   break;
               }
            }

            return allUp;
        }

    }

    /*
        returns <appName>_<appIndex>_<roleName> followed by an underscore.  Expected to be suffixed by a container number
     */
    public String getContainerPrefix(Application app, Container c) throws UninitializedComponentException {
        if (app.getName() == null) {
            throw new UninitializedComponentException("Application Object has no Name");
        } else if (app.getAppIndex() == null) {
            throw new UninitializedComponentException("Application Object has no App Index");
        } else {
            return app.getName().toLowerCase() + "_" + app.getAppIndex().toString() + "_" + c.getName();
        }
    }

    /*
        returns <appName>_<appIndex>_<roleName> followed by an underscore.  Expected to be suffixed by a container number
     */
    public String getContainerPrefix(Application app, String containerName) throws UninitializedComponentException {
        if (app.getName() == null) {
            throw new UninitializedComponentException("Application Object has no Name");
        } else if (app.getAppIndex() == null) {
            throw new UninitializedComponentException("Application Object has no App Index");
        } else if (containerName == null){
            throw new UninitializedComponentException("Container has no Role");
        } else {
            return app.getName().toLowerCase() + "_" + app.getAppIndex().toString() + "_" + containerName;
        }
    }

    public String getNameForNewContainer(Application app, Container c) throws UninitializedComponentException {
        // Name should be <appName>_<appIndex>_<role>_<container index>

        //Integer containerIndex = getNextAvailableContainerIndexFor(app, c);

        //return getContainerPrefix(app, c) + "_" + containerIndex.toString();

        return getContainerPrefix(app, c);
    }

    public Integer getNextAvailableAppIndexFor(Application app) {
        refreshContainerInfo(RefreshType.ALL);

        // looking for the lc app name at the beginning of the string, followed by an underscore, followed by one or more digits.
        Pattern pat = Pattern.compile("^/" + app.getName().toLowerCase() + "_\\d+_.*");
        Integer nextIndex = 1;

        logger.debug("Regex: ^/"+app.getName().toLowerCase() + "_\\d+_.*");
        logger.debug("Containers: "+ containerInfoByName.keySet());

        for (String name : containerInfoByName.keySet()) {
            Matcher m = pat.matcher(name);

            if (m.find()) {
                String[] parts = m.group(0).split("_");

                String indexInUse = parts[1];

                if (indexInUse != null) {
                    Integer index = Integer.parseInt(indexInUse);

                    if (index != null) {
                        Integer proposedNextIndex = ++index;

                        if (proposedNextIndex > nextIndex) {
                            nextIndex = proposedNextIndex;
                        }
                    }
                }
            }
        }

        return nextIndex;
    }

    public Integer getNextAvailableContainerIndexFor(Application app, Container c) throws UninitializedComponentException {
        refreshContainerInfo(RefreshType.ALL);

        // looking for <appName>_<appIndex>_<roleName> followed by an underscore, followed by one or more digits.
        Pattern pat = Pattern.compile("^/" + getContainerPrefix(app, c) + "_\\d+");
        Integer nextIndex = 1;

        for (String name : containerInfoByName.keySet()) {
            Matcher m = pat.matcher(name);

            //logger.debug("Checking "+name);

            if (m.find()) {
                String[] parts = m.group(0).split("_");

                String indexInUse = parts[1];

                //logger.debug("Index in use: "+ indexInUse);

                if (indexInUse != null) {
                    Integer index = Integer.parseInt(indexInUse);

                    if (index != null) {
                        Integer proposedNextIndex = ++index;

                        //logger.debug("Next index: "+ proposedNextIndex);

                        if (proposedNextIndex > nextIndex) {
                            nextIndex = proposedNextIndex;
                        }
                    }
                }
            }
        }

        logger.debug("Returning "+nextIndex);

        return nextIndex;
    }

    public void refreshContainerInfo(RefreshType type) {
        List<String> args = new ArrayList<String>() {{
            add("docker");
            add("ps");
            add("-q");
        }};

        ShellCommand cIdCmd = new ShellCommand().setWorkDir(contextDir.getAbsoluteFile());

        if (type == RefreshType.ALL) {
            args.add("-a");

        }

        cIdCmd.setArgs(args);

        containerInfoById.clear();
        containerInfoByName.clear();

        logger.debug("Getting current container information with command: " + cIdCmd.toString());

        try {
            cIdCmd.exec();

            List<String> idList = cIdCmd.getStdOutAsList();

            logger.debug("Got it");
            logger.debug(idList.size() + " containers on local host");

            for (final String id : idList) {
                ShellCommand cmd = new ShellCommand()
                        .setWorkDir(contextDir.getAbsoluteFile())
                        .setArgs(new ArrayList<String>() {{
                            add("docker");
                            add("inspect");
                            add(id);
                        }});

                logger.debug("Getting container info for "+ id);
                try {
                    cmd.exec();

                    ObjectMapper mapper = new ObjectMapper(new JsonFactory());

                    try {
                        List<LinkedHashMap<String, Object>> containerData = mapper.readValue(
                                cmd.getStdOutAsString(),
                                new TypeReference<List<Map<String, Object>>>() {
                                }
                        );

                        if (containerData != null && containerData.get(0) != null) {
                            containerInfoById.put(id, containerData);
                            containerInfoByName.put((String) containerData.get(0).get("Name"), containerData);
                        }

                    } catch (IOException e) {
                        logger.debug("Couldn't parse output from "+ id +" into an object");
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    logger.debug("docker inspect "+ id + " failed");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    logger.debug("docker inspect " + id + " interrupted");
                }
            }
        } catch (IOException e) {
            logger.debug("docker ps failed");
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.debug("docker ps interrupted");
        }

    }


}
