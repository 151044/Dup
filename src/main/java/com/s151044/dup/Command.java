package com.s151044.dup;

import com.s151044.dup.utils.Env;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

@CommandLine.Command(name = "Dup", mixinStandardHelpOptions = true, version = "0.0.1")
public class Command {
    private static DocumentBuilder builder;
    private static Map<Path,Document> cache = new HashMap<>();
    private static Preferences pref;

    static{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        pref = Preferences.userNodeForPackage(Main.class);
    }

    private boolean hasConfig() {
        Env.OS os = Env.getOS();
        Preferences pref = Preferences.userNodeForPackage(Main.class);
        String confPath = pref.get("config-path", "");
        return Files.exists(os.defaultConfigPath()) || !confPath.isEmpty();
    }
    private Optional<Path> getConfig(){
        if(hasConfig()){
            Preferences pref = Preferences.userNodeForPackage(Main.class);
            String confPath = pref.get("config-path", "");
            if(confPath.isEmpty()){
                Path p = Env.getOS().defaultConfigPath();
                if(Files.exists(p)){
                    return Optional.of(p);
                } else {
                    Optional.empty();
                }
            } else {
                return Optional.of(Path.of(confPath));
            }
        }
        return Optional.empty();
    }
    private Document readDocument(Path p) throws IOException, SAXException {
        if(cache.containsKey(p)){
            return cache.get(p);
        }
        Document docs = builder.parse(p.toFile());
        cache.put(p, docs);
        return docs;
    }
    private boolean isTrue(String toTest){
        return List.of("yes","y","ok","true").contains(toTest.toLowerCase(Locale.ROOT));
    }
    @CommandLine.Command
    public void init(){
        Scanner scan = new Scanner(System.in);
        if(hasConfig()){
            System.out.println("Continuing will delete the previous config file at " + getConfig().get() + "\nContinue?");
            if(!isTrue(scan.nextLine())){
                System.out.println("Exiting per user request.");
                return;
            }
        }
        Path defaultPath = Env.getOS().defaultConfigPath();
        System.out.println("No config file detected.\n" +
                "Enter a new path for the config file, or use the default (" + defaultPath + "): ");
        String response = scan.nextLine();
        if (!response.isEmpty()) {
            defaultPath = Path.of(response);
        }
        System.out.println("Enter the bot token: ");
        String token = scan.nextLine();

        pref.put("config-path", defaultPath.toString());
    }
    @CommandLine.Command
    public void addServer() throws IOException, SAXException {
        if(!hasConfig()){
            System.out.println("Unable to locate config file. Use dup init to generate one.");
            return;
        }
        Document xml = readDocument(getConfig().get());
    }
}
