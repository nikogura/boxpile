package com.nikogura.boxpile.integration.docker;

/**
 * Created by nikogura on 9/7/15.
 */

import com.nikogura.boxpile.Application;
import com.nikogura.boxpile.BuildStrategy;
import com.nikogura.boxpile.Environment;
import com.nikogura.boxpile.application.Container;
import com.nikogura.boxpile.application.container.Exposure;
import com.nikogura.boxpile.exception.BerkshelfException;
import com.nikogura.boxpile.fixtures.DockerInfo;
import com.nikogura.boxpile.util.Functions;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

@SpringBootTest
@DirtiesContext
@Listeners(value = AssumptionListener.class)
public class BuildStrategyTest extends AbstractTestNGSpringContextTests {

    Logger logger = LoggerFactory.getLogger(BuildStrategyTest.class);

    private File  berksfile;
    private String berksfilePath;
    private String databagPath;
    private String attributesFilePath;
    private String testTagName = "testbuild";
    private BuildStrategy bs;
    private Environment env;
    private Application app;
    private Container foo;
    private String testFileResourceDirName = "testfiles";

    @Autowired
    Properties props;

    public boolean dockerAvailable() { return Functions.dockerAvailable(); }

    @BeforeClass
    @Assumption(methods = "dockerAvailable")
    public void setUp() {
        berksfilePath = Functions.getFilePath()+ "Berksfile";
        berksfile = new File(this.getClass().getClassLoader().getResource(berksfilePath).getPath());

        logger.debug("Berksfile path: "+ berksfile.getPath());

        URL url = this.getClass().getClassLoader().getResource("databags");

        if (url == null) {
            logger.debug("Null url");
            fail();
        }

        databagPath = this.getClass().getClassLoader().getResource("databags").getPath();

        attributesFilePath = this.getClass().getClassLoader().getResource(Functions.getFilePath()+ "attributes.json").getPath();

        if (Functions.imageAvailable(testTagName)) {
            fail("Test Image exists: "+ testTagName + " Please remove it before running this test.");
        }

        bs = new BuildStrategy();
        bs.setDockerRepo(DockerInfo.dockerRegistry());
        bs.setDatabagPath(databagPath);
        bs.setAttributesFilePath(attributesFilePath);
        bs.setBerksfilePath(berksfile.getAbsolutePath());


        try {

            env = new Environment("dev");
            String appName = "foo";

            String imageName = "centos";
            String imageTag = "7";
            String baseImageName = Functions.dockerAddress(DockerInfo.dockerRegistry(), imageName, imageTag);

            app = new Application(appName);
            foo = new Container() {{
                setName("foo");
                setBaseImage(baseImageName);
                setRunCommand("/usr/sbin/httpd -k start -DFOREGROUND");
                setExposures(new ArrayList<Exposure>() {{
                    add(new Exposure().setPort(80));
                }});

            }};
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @AfterClass
    @Assumption(methods = "dockerAvailable")
    public void tearDown() {
        // purge container created from test
        try {
            bs.removeImagesBuiltForApp(app);
            bs.dockerRMI(testTagName);
            bs.purgeContextDir();

            if (Functions.imageAvailable(testTagName)) {
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testStageFile() {
        String fileDstName = "foo";

        try {
            bs.stageResource(berksfilePath, fileDstName);

            File testFile = new File(bs.getContextDir() + File.separator + fileDstName);

            assertTrue(testFile.exists(), "File staged to context Dir");

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testStageFileSameName() {
        String fileDst = "Berksfile";

        try {
            bs.stageResource(berksfilePath, fileDst);

            File testFile = new File(bs.getContextDir() + File.separator + fileDst);

            assertTrue(testFile.exists(), "File staged to context Dir");

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testStageDir() {
        List<String> testFiles = new ArrayList<String>() {{
            add("foo");
            add("bar");
            add("baz");
        }};

        URL url = this.getClass().getClassLoader().getResource(testFileResourceDirName);

        String srcDirName = new File(url.getPath()).getAbsolutePath();
        String dstDirName = testFileResourceDirName;

        try {

            bs.stageDir(srcDirName, dstDirName);

            for (String fileName : testFiles) {
                File testFile = new File(bs.getContextDir() + File.separator + dstDirName + File.separator + fileName);
                assertTrue(testFile.exists(), "Created dir contents");
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    @Assumption(methods = "dockerAvailable")
    public void testRunBerkshelf() {
        try {
            logger.debug("Staging Berksfile in " + bs.getContextDir().getAbsolutePath());
            bs.stageFile(berksfile.getPath(), "Berksfile");

            assertTrue(bs.getContextDir().exists(), "Context Dir Exists");

            logger.debug("Running Berkshelf in " + bs.getContextDir().getAbsolutePath());

            bs.runBerkshelf(bs.getContextDir().getAbsolutePath());

        } catch (BerkshelfException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGenerateDockerfile() {
        try {
            String dockerfile = bs.generateDockerfile(app, foo);

            URL url = Thread.currentThread().getContextClassLoader().getResource(DockerInfo.testDockerFileName());

            if (url == null) {
                fail("Can't find data file");
            } else {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(url.getPath()));

                    String testFileContents = "";

                    try {
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();

                        while (line != null) {
                            sb.append(line);
                            sb.append("\n");
                            line = br.readLine();
                        }
                        testFileContents = sb.toString();
                    } finally {
                        br.close();
                    }

                    assertEquals(dockerfile, testFileContents, "Generated dockerfile equals test dockerfile");

                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testWriteDockerfile() {
        try {
            String dockerfile = bs.generateDockerfile(app, foo);

            try {
                bs.writeDockerfile(dockerfile);

                try {
                    BufferedReader br = new BufferedReader(new FileReader(bs.getContextDir().getAbsolutePath()+ File.separator + "Dockerfile"));

                    String testFileContents = "";

                    try {
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();

                        while (line != null) {
                            sb.append(line);
                            sb.append("\n");
                            line = br.readLine();
                        }
                        testFileContents = sb.toString();
                    } finally {
                        br.close();
                    }

                    assertEquals(dockerfile, testFileContents, "Generated dockerfile equals test dockerfile");

                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }


            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    @Test(dependsOnMethods = {"testRunBerkshelf", "testWriteDockerfile"})
    @Assumption(methods = "dockerAvailable")
    public void testDockerBuild() {
        try {
            // copy databags into contextDir/data_bags
            bs.stageDir(databagPath, "data_bags");

            try {
                //copy attribute into contextDir/attributes.json
                bs.stageFile(attributesFilePath, "attributes.json");

                try {
                    //copy Berksfile into contextDir
                    bs.stageFile(berksfile.getAbsolutePath(), "Berksfile");

                    try {
                        bs.dockerBuild(testTagName);

                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }

            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    @Test(dependsOnMethods = {"testDockerBuild"})
    @Assumption(methods = "dockerAvailable")
    public void testBuild() {
        try {
            bs.build(app);
            bs.removeImagesBuiltForApp(app);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}