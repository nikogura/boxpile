package com.nikogura.boxpile.integration.docker;

import com.nikogura.boxpile.Application;
import com.nikogura.boxpile.BuildStrategy;
import com.nikogura.boxpile.LocalRunStrategy;
import com.nikogura.boxpile.Environment;
import com.nikogura.boxpile.application.Container;
import com.nikogura.boxpile.application.container.Exposure;
import com.nikogura.boxpile.application.container.Link;
import com.nikogura.boxpile.application.container.PortMap;
import com.nikogura.boxpile.exception.DockerRunException;
import com.nikogura.boxpile.exception.UninitializedComponentException;
import com.nikogura.boxpile.fixtures.DockerInfo;
import com.nikogura.boxpile.util.Functions;
import com.nikogura.boxpile.util.ShellCommand;
import nl.javadude.assumeng.Assumption;
import nl.javadude.assumeng.AssumptionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

/**
 * Created by nikogura on 9/15/15.
 */
@SpringBootTest
@DirtiesContext
@Listeners(value = AssumptionListener.class)
public class RunStrategyTest extends AbstractTestNGSpringContextTests {
    Logger logger = LoggerFactory.getLogger(RunStrategyTest.class);

    private File berksfile;
    private String berksfilePath;
    private String databagPath;
    private String attributesFilePath;
    private String testTagName = "testbuild";
    private BuildStrategy bs;
    private LocalRunStrategy rs;
    private Environment env;
    private Container testContainer;
    private String testFileResourceDirName = "testfiles";

    private Container memcached;
    String memcachedName = "memcached";

    @Autowired
    Properties props;

    Application artApp;
    String artAppName = "apache";

    Application devApp;

    public boolean dockerAvailable() {
        return Functions.dockerAvailable();
    }

    @BeforeClass
    @Assumption(methods = "dockerAvailable")
    public void setUp() {
        berksfilePath = Functions.getFilePath() + "Berksfile";
        berksfile = new File(this.getClass().getClassLoader().getResource(berksfilePath).getPath());

        logger.debug("Berksfile path: " + berksfile.getPath());

        URL url = this.getClass().getClassLoader().getResource("databags");

        if (url == null) {
            logger.debug("Null url");
            fail();
        }

        databagPath = this.getClass().getClassLoader().getResource("databags").getPath();

        attributesFilePath = this.getClass().getClassLoader().getResource(Functions.getFilePath() + "attributes.json").getPath();

        if (Functions.imageAvailable(testTagName)) {
            fail("Test Image exists: " + testTagName + " Please remove it before running this test.");
        }

        bs = new BuildStrategy();
        bs.setDockerRepo(DockerInfo.dockerRegistry());
        bs.setDatabagPath(databagPath);
        bs.setAttributesFilePath(attributesFilePath);
        bs.setBerksfilePath(berksfile.getAbsolutePath());

        try {
            env = new Environment("dev");

            String imageName = "centos";
            String imageTag = "7";
            String baseImageName = Functions.dockerAddress(DockerInfo.dockerRegistry(), imageName, imageTag);

            artApp = new Application(artAppName);
            artApp.setEnv(env);
            artApp.setAppIndex(1);

            testContainer = new Container() {{
                setName(artAppName);
                setBaseImage(baseImageName);
                setRunCommand("/usr/sbin/httpd -k start -DFOREGROUND");
                setExposures(new ArrayList<Exposure>() {{
                    add(new Exposure().setPort(80));
                }});
                setPorts(new ArrayList<PortMap>() {{
                    add(new PortMap().setHost(8080).setContainer(80));
                }});
                setLinks(new ArrayList<Link>() {{
                    add(new Link().setAlias(memcachedName).setContainerName(memcachedName));
                }});

            }};


            memcached = new Container() {{
                setName(memcachedName);
                setImage(Functions.dockerAddress(DockerInfo.dockerRegistry(), memcachedName));
                setExposures(new ArrayList<Exposure>() {{
                    add(new Exposure().setPort(11211));
                }});

            }};

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        rs = new LocalRunStrategy();
    }

    @AfterClass
    public void tearDown() {
        //clean up the built containter when we're done
        try {
            logger.debug("Cleaning up Images");
            bs.removeImagesBuiltForApp(artApp);
            //bs.removeImagesBuiltForApp(devApp);

            if (Functions.imageAvailable(testTagName)) {
                fail();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    // needs to be run before any containers are running, or results may vary
    @Test
    public void testAllDepsRunning() {
        try {
            assertTrue(rs.allDepsRunning(artApp, memcached), "All Dependencies for Memcached are running");

            //assertFalse(rs.allDepsRunning(artApp, phoenix), "All Dependencies for Phoenix are NOT Running");
        } catch (UninitializedComponentException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testGetContainerPrefix() {
        try {
            String prefix = rs.getContainerPrefix(artApp, artAppName);

            logger.debug("Container Prefix: + prefix");

            String pattString = "\\w+_\\d+_\\w+";

            assertTrue(Pattern.matches(pattString, prefix), "Container Prefix matches pattern " + pattString);

            // try it another way
            prefix = rs.getContainerPrefix(artApp, artAppName);

            assertTrue(Pattern.matches(pattString, prefix), "Container Prefix matches pattern " + pattString);

        } catch (UninitializedComponentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetNextAvailableAppIndexFor() {
        Integer actual = rs.getNextAvailableAppIndexFor(artApp);

        assertTrue(actual > 0, "Next Available App Index is greater than 0");
    }

    @Test
    public void testGetNextAvailableContainerIndexFor() {
        try {
            Integer actual = rs.getNextAvailableContainerIndexFor(artApp, testContainer);

            assertTrue(actual > 0, "Next Available Container Index is greater than 0");
        } catch (UninitializedComponentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetNameForContainer() {
        try {
            String containerName = rs.getNameForNewContainer(artApp, testContainer);

            String pattString = "\\w+_\\d+_\\w+";

            assertTrue(Pattern.matches(pattString, containerName), "Container Prefix matches pattern " + pattString);
        } catch (UninitializedComponentException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testCreateDockerRunCommandArtifact() {
        try {
            String containerName = rs.getNameForNewContainer(artApp, testContainer);
            ShellCommand command = rs.createDockerRunCommand(artApp, testContainer, containerName);

            logger.debug("Run Command: ");
            logger.debug(command.toString());

            String expectedCommand = "docker run -d -p 8080:80 --link memcached:memcached --name apache_1_apache apache-dev "; // trailing space is significant

            assertEquals(command.toString(), expectedCommand, "Generated Docker Run command matches example command");

        } catch (UninitializedComponentException e) {
            e.printStackTrace();
            fail();
        }

    }

//    @Test
//    public void testCreateDockerRunCommandDeveloper() {
//        try {
//            String containerName = rp.getNameForNewContainer(devApp, devPhoenix);
//            ShellCommand command = rp.createDockerRunCommand(devApp, devPhoenix, containerName);
//
//            logger.debug("Name: " + containerName);
//            logger.debug("Run Command: ");
//            logger.debug(command);
//
//            String artifactCommand = "docker run -d -p 47001:47001 --link kvtool:kvtool --link nastool:nastool --link psctool:psctool --link memcached:memcached --name phoenix_1_phoenix phoenix-ncit "; // trailing space is significant
//
//            String runDir = StringUtils.chomp(System.getProperty("user.dir"));
//
//            String developerCommand = "docker run -d -p 47001:47001 -v "+ runDir +"/target/PhoenixHome:/apps/Phoenix/Current/PhoenixHome --link kvtool:kvtool --link nastool:nastool --link psctool:psctool --link memcached:memcached --name phoenix_1_phoenix phoenix-ncit "; // trailing space is significant
//            assertNotEquals(command.toString(), artifactCommand, "Generated Docker Run command does not match artifact command");
//            assertEquals(command.toString(), developerCommand, "Generated Docker Run command matches developer command");
//
//        } catch (UninitializedComponentException e) {
//            e.printStackTrace();
//            fail();
//        }
//
//    }

    @Test(dependsOnMethods = {"testAllDepsRunning"})
    public void testRunContainer() {
        try {
            env = new Environment("dev");

            Application app = new Application(memcached.getName());
            app.setName(memcached.getName());
            app.setEnv(env);

            app.setAppIndex(rs.getNextAvailableAppIndexFor(app));

            app.setContainers(new LinkedHashMap<String, Container>() {{
                put(memcachedName, memcached);
            }});

            if (Functions.imageAvailable(memcached.getImage())) {
                try {
                    String containerName = rs.runContainer(app, memcached);

                    logger.debug("Container Name: " + containerName);

                    logger.debug("Checking to see if " + memcached.getName());
                    if (!rs.containerRunning(app, memcached)) {
                        logger.debug("Container not Running");
                        fail();
                    }

                    try {
                        rs.killContainer(containerName);

                    } catch (InterruptedException e) {
                        logger.debug("Run Job Interrupted");
                        e.printStackTrace();
                        fail();
                    } catch (IOException e) {
                        logger.debug("Failure Stopping Container: " + memcached.getName());
                        e.printStackTrace();
                        fail();
                    }

                    try {
                        rs.rmContainer(containerName);

                    } catch (InterruptedException e) {
                        logger.debug("Run Job Interrupted");
                        logger.debug("Failure Removing Container: " + memcached.getName());
                        e.printStackTrace();
                        fail();
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail();
                    }

                } catch (InterruptedException e) {
                    logger.debug("Run Job Interrupted");
                    e.printStackTrace();
                    fail();
                } catch (IOException e) {
                    logger.debug("Failure Running Container: " + memcached.getName());
                    e.printStackTrace();
                    fail();
                } catch (UninitializedComponentException e) {
                    e.printStackTrace();
                    fail();
                }

            } else {
                logger.debug("No Image available for: " + memcached.getName());
                fail();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(dependsOnMethods={"testRunContainer"} )
    public void testRunContainers() {
        try {
            env = new Environment("dev");

            Application app = new Application(artAppName);
            app.setName(artAppName);
            app.setEnv(env);
            app.setAppIndex(rs.getNextAvailableAppIndexFor(app));
            app.setContainers(new LinkedHashMap<String, Container>() {{
                put(artAppName, testContainer);
                put(memcached.getName(), memcached);
            }});

            if (!Functions.imageAvailable(testTagName)) {
                try {
                    logger.debug("Have to BuildStrategy Image: "+ testTagName);
                    bs.build(app);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }

            }

            Set<String> containerNames = new HashSet<String>();

            try {
                containerNames = rs.runContainers(app);

                for (Container c : app.getContainers().values()) {
                    assertTrue(rs.containerRunning(app, c), "Check if " + c.getName() + "  is running");
                }

                try {
                    rs.killContainers(containerNames);
                } catch (DockerRunException e) {
                    e.printStackTrace();
                    fail();
                }

                try {
                    rs.rmContainers(containerNames);
                } catch (DockerRunException e) {
                    e.printStackTrace();
                    fail();
                }

                try {
                    bs.removeImagesBuiltForApp(app);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }

            } catch (DockerRunException e) {

                e.printStackTrace();
                try {
                    rs.killContainers(containerNames);
                } catch (DockerRunException e1) {

                }

                try {
                    rs.rmContainers(containerNames);
                } catch (DockerRunException e1) {

                }

                fail();
            } catch (UninitializedComponentException e) {
                e.printStackTrace();
                fail();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(dependsOnMethods = {"testRunContainers"})
    public void testRun() {
        try {
            env = new Environment("dev");

            Application app = new Application(artAppName);
            app.setName(artAppName);
            app.setEnv(env);
            app.setAppIndex(rs.getNextAvailableAppIndexFor(app));
            app.setContainers(new LinkedHashMap<String, Container>() {{
                put(artAppName, testContainer);
                put(memcached.getName(), memcached);
            }});

            if (!Functions.imageAvailable(testTagName)) {
                try {
                    logger.debug("Have to BuildStrategy Image: "+ testTagName);
                    bs.build(app);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }

            }

            try {
                logger.debug("Running App");
                rs.run(app);

                try {
                    assertTrue(rs.containersRunning(app), "All Containers Running");

                    logger.debug("Killing App");
                    rs.kill(app);

                    logger.debug("Removing App");
                    rs.rm(app);

                    try {
                        bs.removeImagesBuiltForApp(app);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }

                } catch (UninitializedComponentException e) {
                    e.printStackTrace();
                    fail();
                }

            } catch (DockerRunException e) {
                e.printStackTrace();

                try {
                    rs.kill(app);
                } catch (DockerRunException e1) {
                    e1.printStackTrace();
                }

                try {
                    rs.rm(app);
                } catch (DockerRunException e1) {
                    e1.printStackTrace();
                }

                fail();
            } catch (UninitializedComponentException e) {
                e.printStackTrace();
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
