package com.s151044.dup.utils;

import java.nio.file.Path;

/**
 * A utility class for learning about the running environment.
 */
public class Env {
    private static final OS os;

    static {
        String osStr = System.getProperty("os.name");
        if (osStr == null) {
            os = OS.UNKNOWN;
        } else if (osStr.contains("Linux")) {
            os = OS.LINUX;
        } else if (osStr.toLowerCase().contains("windows")) {
            os = OS.WINDOWS;
        } else if (osStr.toLowerCase().contains("mac")) {
            os = OS.MAC;
        } else {
            os = OS.UNKNOWN;
        }
    }

    private Env() {
        throw new AssertionError();
    }

    public static OS getOS() {
        return os;
    }

    public enum OS {
        WINDOWS, MAC, LINUX(System.getProperty("user.home") + ".config/dup.xml"), UNKNOWN;
        private Path path;
        OS(){
            path = Path.of(System.getProperty("user.dir") + System.getProperty("file.separator") + "dup.xml");
        }
        OS(String path){
            this.path = Path.of(path);
        }
        public Path defaultConfigPath(){
            return path;
        }
    }
}
