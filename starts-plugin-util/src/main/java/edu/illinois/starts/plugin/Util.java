package edu.illinois.starts.plugin;

import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.DefaultScanResult;

import java.io.File;
import java.util.List;

public class Util {
    public static List<String> getTestClasses(File testClassDir) {
        DirectoryScanner scanner = new DirectoryScanner(testClassDir, TestListResolver.getEmptyTestListResolver());
        DefaultScanResult defaultScanResult = scanner.scan();
        return (List<String>) defaultScanResult.getFiles();
    }
}