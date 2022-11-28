package edu.illinois.starts.plugin.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the starts task
 */
class StartsTaskTest {
    @TempDir
    static File projectDir;

    GradleRunner runner;

    private static File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private static File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }


    private static void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }

    @BeforeAll
    public static void setupTest() throws IOException {
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(), """
                        plugins {
                            id 'java'
                            id 'edu.illinois.starts' version '1.4-SNAPSHOT'
                        }
                         
                        group 'base'
                        version '1.0-SNAPSHOT'
                        
                        repositories {
                            mavenCentral()
                            mavenLocal()
                        }
                        
                        dependencies {
                            testImplementation 'junit:junit:4.13.2'
                        }
                        
                        test {
                            useJUnit()
                        
                            testLogging {
                                events "PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR"
                            }
                        }""".stripIndent());
    }

    @BeforeEach
    public void setupTests() {
        runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withProjectDir(projectDir);
        runner.withArguments("starts");
    }

    public File createFile(String name, String... path) throws IOException {
        File dir = Paths.get(projectDir.toString(), path).toFile();
        assertTrue(dir.mkdirs());
        File file = new File(dir, name);
        assertTrue(file.createNewFile());
        return file;
    }

    @Test
    public void baseIT() throws IOException {
        File source = createFile("Simple.java", "base-it", "src", "main", "java", "base");
        writeString(source, """
                package base;
                
                import java.util.LinkedHashSet;
                import java.util.Set;
                
                public class Simple {
                    private LinkedHashSet output;
                
                    public Simple() {
                        super();
                        output = new LinkedHashSet();
                    }
                
                    public void add(int addend) {
                        output.add(addend);
                    }
                
                    public Set<Integer> getSet() {
                        return output;
                    }
                
                }""".stripIndent());
        File test = createFile("SimpleTest.java", "base-it", "src", "test", "java", "base");
        writeString(test, """
                package base;
                
                import static org.junit.Assert.assertEquals;
                
                import java.util.Set;
                
                import org.junit.Test;
                
                public class SimpleTest {
                
                    @Test
                    public void test() {
                        Simple simple = new Simple();
                        simple.add(1);
                        simple.add(2);
                        simple.add(3);
                        Set<Integer> out = simple.getSet();
                        int result = 0;
                        for(Integer i : out) {
                            result += i;
                        }
                        assertEquals("sum", 6, result);
                    }
                
                }""".stripIndent());
        BuildResult result = runner.build();
        assertTrue(result.getOutput().contains("INFO: ********** Run **********"));
    }
}
