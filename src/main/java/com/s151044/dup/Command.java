package com.s151044.dup;

import com.s151044.dup.utils.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

@CommandLine.Command(name = "Dup", mixinStandardHelpOptions = true, version = "0.0.1")
public class Command {
    private static DocumentBuilder builder;
    private static Map<Path,Document> cache = new HashMap<>();
    private static Preferences pref;
    private static TransformerFactory transformFactory;

    static{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        transformFactory = TransformerFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        pref = Preferences.userNodeForPackage(Main.class);
    }

    // Private Helpers
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

    private void writeXml(Document doc, OutputStream out) {
        try {
            Transformer trans = transformFactory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT,"yes");
            trans.transform(new DOMSource(doc), new StreamResult(out));
        } catch (TransformerException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Element addIdInfo(Document doc, Element toAdd, String id, boolean defaults){
        Element defId = doc.createElement("id");
        defId.setTextContent(id);
        Element defElement = doc.createElement("default");
        defElement.setTextContent(Boolean.toString(defaults));
        toAdd.appendChild(defId);
        toAdd.appendChild(defElement);
        return toAdd;
    }

    private NodeList getNodesUnder(Node n, String toSearch){
        return ((Element) n).getElementsByTagName(toSearch);
    }
    //Commands
    @CommandLine.Command
    public void init() throws FileNotFoundException {
        Scanner scan = new Scanner(System.in);
        if(hasConfig()){
            System.out.print("Continuing will delete the previous config file at " + getConfig().get() + "\nContinue?");
            if(!isTrue(scan.nextLine())){
                System.out.println("Exiting per user request.");
                return;
            }
        }
        Path defaultPath = Env.getOS().defaultConfigPath();
        System.out.print("No config file detected.\n" +
                "Enter a new path for the config file, or use the default (" + defaultPath + "): ");
        String response = scan.nextLine();
        if (!response.isEmpty()) {
            defaultPath = Path.of(response);
        }
        System.out.print("Enter the bot token: ");
        String token = scan.nextLine();
        System.out.print("Enter the bot owner's ID: ");
        String ownerId = scan.nextLine();
        System.out.print("Would you like to select a default server and channel? ");
        boolean hasDefault = isTrue(scan.nextLine());
        String serverId = "", channelId = "";
        if(hasDefault){
            System.out.print("Enter a server ID: ");
            serverId = scan.nextLine();
            System.out.print("Enter a channel ID: ");
            channelId = scan.nextLine();
        }

        //Creating XML
        Document doc = builder.newDocument();
        Element root = doc.createElement("dup");
        doc.appendChild(root);
        Element bot = doc.createElement("bot");
        root.appendChild(bot);
        Element tokenElement = doc.createElement("token");
        tokenElement.setTextContent(token);
        Element ownerElement = doc.createElement("owner");
        ownerElement.setTextContent(ownerId);
        bot.appendChild(tokenElement);
        bot.appendChild(ownerElement);
        Element serverElement = doc.createElement("servers");
        Element channelElement = doc.createElement("channels");
        root.appendChild(serverElement);
        root.appendChild(channelElement);
        if(hasDefault){
            serverElement.appendChild(addIdInfo(doc, doc.createElement("server"), serverId, true));
            channelElement.appendChild(addIdInfo(doc, doc.createElement("channel"), channelId, true));
        }

        writeXml(doc, new FileOutputStream(defaultPath.toFile()));
        pref.put("config-path", defaultPath.toString());
        System.out.println("Setup complete.");
    }

    @CommandLine.Command
    public void addChannel() throws IOException, SAXException {
        if(!hasConfig()){
            System.out.println("Unable to locate config file. Use dup init to generate one.");
            return;
        }
        Document xml = readDocument(getConfig().get());
        NodeList botOwns = xml.getDocumentElement().getElementsByTagName("bot");
        String token = getNodesUnder(botOwns.item(0), "token").item(0).getTextContent();
        String owner = getNodesUnder(botOwns.item(0), "owner").item(0).getTextContent();
    }
}
