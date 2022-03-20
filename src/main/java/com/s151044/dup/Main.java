package com.s151044.dup;

import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        CommandLine cmd = new CommandLine(new Command());
        cmd.execute(args);
    }
}
