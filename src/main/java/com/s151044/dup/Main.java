package com.s151044.dup;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Command());
        cmd.execute(args);
    }
}
