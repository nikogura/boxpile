package com.nikogura.boxpile.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nikogura on 9/17/15.
 */
public class ShellCommand {
    String path;
    List<String> args = new ArrayList<String>();
    File workDir;
    String stdOutAsString;
    String stdErrAsString;
    List<String> stdOutAsList = new ArrayList<String>();
    List<String> stdErrAsList = new ArrayList<String>();
    Boolean printStdOut = false;
    Boolean printStdErr = false;
    Map<String, String> runEnv = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(ShellCommand.class);

    public Map<String, String> getRunEnv() {
        return runEnv;
    }

    public ShellCommand setRunEnv(Map<String, String> runEnv) {
        this.runEnv = runEnv;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ShellCommand setPath(String path) {
        this.path = path;
        return this;
    }

    public List<String> getArgs() {
        return args;
    }

    public ShellCommand setArgs(List<String> args) {
        this.args = args;
        return this;
    }

    public File getWorkDir() {
        return workDir;
    }

    public ShellCommand setWorkDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    public ShellCommand setWorkDir(String workDirPath) {
        File workDir = new File(workDirPath);
        this.workDir = workDir;
        return this;
    }

    public String getStdOutAsString() {
        return stdOutAsString;
    }

    public String getStdErrAsString() {
        return stdErrAsString;
    }

    public List<String> getStdOutAsList() {
        return stdOutAsList;
    }

    public List<String> getStdErrAsList() {
        return stdErrAsList;
    }

    public Boolean getPrintStdOut() {
        return printStdOut;
    }

    public ShellCommand setPrintStdOut(Boolean printStdOut) {
        this.printStdOut = printStdOut;
        return this;
    }

    public Boolean getPrintStdErr() {
        return printStdErr;
    }

    public ShellCommand setPrintStdErr(Boolean printStdErr) {
        this.printStdErr = printStdErr;
        return this;
    }

    public boolean exec() throws InterruptedException, IOException {
        if (this.getWorkDir() == null ) {
            throw new IOException("Null Work Dir.  ShellCommand must be provided with a workdir via setWorkDir() to function!");
        } else {
            logger.trace("Creating ProcessBuilder");
            ProcessBuilder builder = new ProcessBuilder(args);

            if (!this.getRunEnv().isEmpty()) {
                Map<String, String> env = builder.environment();
                for (String key : this.getRunEnv().keySet()) {
                    env.put(key, this.getRunEnv().get(key));
                }
            }

            logger.trace("setting work dir");
            builder.directory(this.getWorkDir().getAbsoluteFile());

            logger.trace("Starting job");
            Process job = builder.start();

            logger.trace("Capturing Streams, but not crossing them");
            BufferedReader brout = new BufferedReader(new InputStreamReader(job.getInputStream()));
            BufferedReader brerr = new BufferedReader(new InputStreamReader(job.getErrorStream()));

            String line = null;
            String previous = null;

            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();

            logger.trace("STDOUT:");
            while ((line = brout.readLine()) != null) {
                if (!line.equals(previous)) {
                    previous = line;
                    //logger.debug(line);
                    out.append(line).append("\n");
                    stdOutAsList.add(line);
                    if (printStdOut) {
                        System.out.println(line);
                    }
                }
            }

            logger.trace("STERR:");
            while ((line = brerr.readLine()) != null) {
                if (!line.equals(previous)) {
                    previous = line;
                    //logger.debug(line);
                    err.append(line).append("\n");
                    stdErrAsList.add(line);
                    if (printStdErr) {
                        System.out.println(line);
                    }
                }
            }

            if (job.waitFor() == 0) {
                stdOutAsString = out.toString();
                stdErrAsString = err.toString();

                return true;
            } else {
                stdOutAsString = out.toString();
                stdErrAsString = err.toString();

                throw new IOException("Command Ran, but with errors.  Command: "+ this.toString() + "Error: " + err.toString());
            }

        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShellCommand that = (ShellCommand) o;

        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (printStdOut != null ? !printStdOut.equals(that.printStdOut) : that.printStdOut != null) return false;
        if (args != null ? !args.equals(that.args) : that.args != null) return false;
        return !(workDir != null ? !workDir.equals(that.workDir) : that.workDir != null);

    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (args != null ? args.hashCode() : 0);
        result = 31 * result + (printStdOut != null ? printStdOut.hashCode() : 0);
        result = 31 * result + (workDir != null ? workDir.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg);
            sb.append(" ");
        }

        return sb.toString();
    }
}
