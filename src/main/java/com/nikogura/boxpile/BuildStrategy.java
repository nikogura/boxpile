package com.nikogura.boxpile;

import com.nikogura.boxpile.application.Container;
import com.nikogura.boxpile.application.container.Volume;
import com.nikogura.boxpile.exception.BerkshelfException;
import com.nikogura.boxpile.exception.DockerBuildException;
import com.nikogura.boxpile.exception.DockerRMIException;
import com.nikogura.boxpile.util.Functions;
import com.nikogura.boxpile.util.ShellCommand;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nik.ogura@gmail.com
 *
 * Takes 'applications' and executes requisite docker commmands to build the various containers
 * Produces both dockerfiles and docker images
 */
public class BuildStrategy {
    String databagPath;
    String attributesFilePath;
    String berksfilePath;        // where to find berksfile we're passing in
    String dockerRepo;
    File contextDir;
    Boolean quiet = false;

    private static final Logger logger = LoggerFactory.getLogger(BuildStrategy.class);

    public BuildStrategy() {
        String tmpRoot = System.getProperty("java.io.tmpdir");
        String tmpDir = "boxpile";
        contextDir = new File(tmpRoot + File.separator + tmpDir);
        contextDir.mkdir();

        // create subdirs
        for (String dir : new ArrayList<String>() {{
            add("data_bags");
        }}) {
            File dirFile = new File(contextDir.getPath() + File.separator + dir);
            dirFile.mkdir();
        }

    }

    public Boolean getQuiet() {
        return quiet;
    }

    public BuildStrategy setQuiet(Boolean quiet) {
        this.quiet = quiet;
        return this;
    }

    public String getDockerRepo() {
        return dockerRepo;
    }

    public BuildStrategy setDockerRepo(String dockerRepo) {
        this.dockerRepo = dockerRepo;
        return this;
    }

    public File getContextDir() {
        return contextDir;
    }

    public BuildStrategy setContextDir(File contextDir) {
        this.contextDir = contextDir;
        return this;
    }

    public String getDatabagPath() {
        return databagPath;
    }

    public BuildStrategy setDatabagPath(String databagPath) {
        this.databagPath = databagPath;
        return this;
    }

    public String getAttributesFilePath() {
        return attributesFilePath;
    }

    public BuildStrategy setAttributesFilePath(String attributesFilePath) {
        this.attributesFilePath = attributesFilePath;
        return this;
    }

    public String getBerksfilePath() {
        return berksfilePath;
    }

    public BuildStrategy setBerksfilePath(String berksfilePath) {
        this.berksfilePath = berksfilePath;
        return this;
    }

    public void build(Application app) throws IOException, DockerBuildException {

        logger.debug("Starting build");

        // have to have paths for private props and hiera data, otherwise we can't build
        //if (this.getPrivatePropsPath() == null) {
        //    throw new DockerBuildException("Cannot build without a Private Props Path");
        //}

        if (this.getAttributesFilePath() == null) {
            throw new DockerBuildException("Cannot build without an Attributes File");
        }

        logger.debug("Passed checks.  Running.");

        logger.debug("Staging Files");

        logger.debug("Staging Databags from: "+ databagPath);
        stageDir(databagPath, "data_bags");

        logger.debug("Staging Attributes: " + attributesFilePath);
        stageFile(attributesFilePath, "attributes.json");

        logger.debug("Staging Berksfile: "+ berksfilePath);
        stageFile(berksfilePath, "Berksfile");

        try {
            logger.debug("Running Berkshelf");
            runBerkshelf(contextDir.getAbsolutePath());

            logger.debug("Looping through Containers");
            // create the containers
            for (Container c : app.getContainers().values()) {
                // skip it if it uses an image.  Images are already built
                logger.debug("Checking to see if we have to build container: "+ c.getName());
                if (c.getImage() == null) {
                    logger.debug("Preparing to build container: "+ c.getName());

                    String imageName = c.getName() + "-" + app.getEnv().getName();

                    logger.debug("Generated Tag: " + imageName);

                    // stage buildVolumes
                    stageBuildVolumes(c);

                    // generate dockerfile, write into $tmpdir
                    String dockerfile = generateDockerfile(app, c);

                    // Write the dockerfile
                    writeDockerfile(dockerfile);

                    logger.debug("Building Tag: " + imageName);

                    // run docker build
                    dockerBuild(imageName);

                    logger.debug("Adding " + imageName + " to list of built images for this app");
                    app.getBuiltImages().add(imageName);

                    logger.debug("Images so far for this app:  " + app.getBuiltImages());

                    logger.debug("cleaning up build volumes");

                    cleanupBuildVolumes(c);

                    logger.debug("BuildStrategy Complete");
                }
            }
        } catch (BerkshelfException e) {
            e.printStackTrace();
        }
    }

    public void purgeContextDir() {
        try {
            FileUtils.deleteDirectory(contextDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stageDir(String srcName, String dstName) throws IOException {
        File src = new File(srcName);
        File dst = new File(contextDir.getAbsolutePath() + File.separator + dstName);

        FileUtils.copyDirectory(src, dst);

    }

    public void stageFile(String srcName, String dstName) throws IOException {
        File src = new File(srcName);
        File dst = new File(contextDir.getAbsolutePath() + File.separator + dstName);

        FileUtils.copyFile(src, dst);

    }

    public void stageResource(String srcName, String dstName) throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(srcName);
        File dst = new File(contextDir, dstName);

        logger.debug("Destination: "+ dst.getAbsolutePath());

        if (!dst.exists()) {
            dst.createNewFile();
        }

        OutputStream out = new FileOutputStream(dst);

        IOUtils.copy(in, out);

        in.close();
        out.close();

    }

    public String generateDockerfile(Application app, Container c) {

        String fileName = "com/nikogura/boxpile/templates/dockerfile.stg";
        STGroupFile file = new STGroupFile(fileName);
        ST template = file.getInstanceOf("dockerfile");

        template.add("application", app);
        template.add("container", c);

        return template.render();
    }

    public void writeDockerfile(String content) throws IOException {
        File dockerfile = new File(this.getContextDir().getAbsolutePath() + File.separator + "Dockerfile");

        // if file doesnt exists, then create it
        if (!dockerfile.exists()) {
            dockerfile.createNewFile();
        }

        FileWriter fw = new FileWriter(dockerfile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.close();
    }

    public boolean runBerkshelf(String path) throws BerkshelfException {

        String cbPath = path + "/cookbooks";

        ShellCommand cmd = new ShellCommand()
                .setArgs(new ArrayList<String>(){{
                    add("berks");
                    add("vendor");
                    add(cbPath);
                }})
                .setWorkDir(contextDir.getAbsoluteFile());

        if (!quiet) {
            cmd.setPrintStdOut(true);
            cmd.setPrintStdErr(true);
        }

        try {
            if (cmd.exec()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BerkshelfException("Berkshelf Ran, but with errors: " + cmd.getStdErrAsString());
        }

        return false;

    }

    public boolean dockerBuild(final String imageName) throws IOException, DockerBuildException {
        ShellCommand cmd = new ShellCommand()
                .setArgs(new ArrayList<String>(){{
                    add("docker");
                    add("build");
                    add("-t");
                    add(imageName);
                    add(".");
                }})
                .setWorkDir(contextDir.getAbsoluteFile());

        if (!quiet) {
            System.out.println("Docker BuildStrategy Command: " + cmd.toString());
            cmd.setPrintStdOut(true);
        }

        try {
            if (cmd.exec()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DockerBuildException("Docker BuildStrategy Ran, but with errors: "+ cmd.getStdErrAsString());
        }

        return false;
    }

    public boolean removeImagesBuiltForApp(Application app) throws IOException, DockerRMIException {
        logger.debug("Removing Images for "+ app.getName());
        logger.debug("Built Images: "+ app.getBuiltImages());
        for (String imageName : app.getBuiltImages()) {
            logger.debug("Attempting to remove "+ imageName);
            if (Functions.imageAvailable(imageName)) {
                logger.debug("Here goes....");
                if (!dockerRMI(imageName)) {
                    return false;
                } else {
                    logger.debug("Done.");
                }
            } else {
                logger.debug("That's funny, " + imageName + " isn't built");
            }
        }

       return true;
    }

    public boolean dockerRMI(final String imageName) throws IOException, DockerRMIException {
        logger.debug("Removing Image:" + imageName);

        ShellCommand cmd = new ShellCommand()
                .setArgs(new ArrayList<String>(){{
                        add("docker");
                        add("rmi");
                        add("-f");
                        add(imageName);
                    }})
                .setWorkDir(contextDir.getAbsoluteFile());

        try {
            if (cmd.exec()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DockerRMIException("Docker RMI Ran, but with errors: "+ cmd.getStdErrAsString());
        }

        return false;
    }

    public String getBuildVolumeStageName(Volume vol) {
        return vol.getContainerPath().substring(vol.getContainerPath().lastIndexOf("/") + 1);
    }

    public void stageBuildVolumes(Container c) throws IOException {
        String workingDir = System.getProperty("user.dir");
        String relativeDirPattern = "^\\.\\.?\\/(.*)";

        for (Volume vol : c.getBuildVolumes()) {

            Matcher m = Pattern.compile(relativeDirPattern).matcher(vol.getSourcePath());

            // if we have a relative path of any sort, we need to get the actual dir this baby was run from
            if (m.find()) {
                // Take the sourcePath, stage it into the build context.  Set the buildSourcePath on the volume
                logger.debug("We have a relative path: "+ vol.getSourcePath());

                String ctxPath = getBuildVolumeStageName(vol);

                Path basePath = FileSystems.getDefault().getPath(workingDir);
                Path srcPath = basePath.resolve(vol.getSourcePath());

                stageDir(srcPath.toString(), ctxPath);
                vol.setBuildSourcePath(ctxPath);

            } else {
                stageDir(vol.getSourcePath(), getBuildVolumeStageName(vol));
            }
        }
    }

    public void cleanupBuildVolumes(Container c) throws IOException {
        for (Volume vol : c.getBuildVolumes()) {
            File stageVol = new File(contextDir, getBuildVolumeStageName(vol));
            FileUtils.deleteDirectory(stageVol);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildStrategy that = (BuildStrategy) o;

        if (databagPath != null ? !databagPath.equals(that.databagPath) : that.databagPath != null) return false;
        if (attributesFilePath != null ? !attributesFilePath.equals(that.attributesFilePath) : that.attributesFilePath != null)
            return false;
        if (berksfilePath != null ? !berksfilePath.equals(that.berksfilePath) : that.berksfilePath != null)
            return false;
        if (dockerRepo != null ? !dockerRepo.equals(that.dockerRepo) : that.dockerRepo != null) return false;
        if (contextDir != null ? !contextDir.equals(that.contextDir) : that.contextDir != null) return false;
        return quiet != null ? quiet.equals(that.quiet) : that.quiet == null;

    }

    @Override
    public int hashCode() {
        int result = databagPath != null ? databagPath.hashCode() : 0;
        result = 31 * result + (attributesFilePath != null ? attributesFilePath.hashCode() : 0);
        result = 31 * result + (berksfilePath != null ? berksfilePath.hashCode() : 0);
        result = 31 * result + (dockerRepo != null ? dockerRepo.hashCode() : 0);
        result = 31 * result + (contextDir != null ? contextDir.hashCode() : 0);
        result = 31 * result + (quiet != null ? quiet.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DockerBuildStrategy{" +
                " databagPath='" + databagPath + '\'' + ",\n" +
                " attributesFilePath='" + attributesFilePath + '\'' + ",\n" +
                " berksfilePath='" + berksfilePath + '\'' + ",\n" +
                " dockerRepo='" + dockerRepo + '\'' + ",\n" +
                " contextDir=" + contextDir + "\n" +
                " quiet=" + quiet + "\n" +
                '}';
    }
}
