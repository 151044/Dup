package com.s151044.dup;

import com.s151044.dup.discord.Bot;
import com.s151044.dup.utils.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.security.auth.login.LoginException;
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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

@CommandLine.Command(name = "Dup", mixinStandardHelpOptions = true, version = "0.0.1")
public class Command {
    private static DocumentBuilder builder;
    private static Map<Path,Document> cache = new HashMap<>();
    private static Preferences pref;
    private static TransformerFactory transformFactory;
    private boolean botInitialized = false;
    private Bot bot;
    private List<Target> targets = new ArrayList<>();

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

    private void initBot(String token, String owner){
        bot = new Bot(token, owner);
        try {
            if(bot.login()){
                botInitialized = true;
               return;
            }
        } catch (LoginException e) {
            System.out.println(e);
        }
        System.out.println("Failed to login to Discord!");
        System.exit(1);
    }

    private void initBot(Document xml){
        NodeList botOwns = xml.getDocumentElement().getElementsByTagName("bot");
        String token = getNodesUnder(botOwns, "token",0).item(0).getTextContent();
        String owner = getNodesUnder(botOwns, "owner",0).item(0).getTextContent();
        initBot(token, owner);
    }

    private void initTargets(Document xml){
        NodeList targeted = xml.getDocumentElement().getElementsByTagName("targets");
        targeted = ((Element) targeted.item(0)).getElementsByTagName("target");
        for(int i = 0; i < targeted.getLength(); i++){
            String serverId = getNodesUnder(targeted, "serverId",i).item(0).getTextContent();
            String channelId = getNodesUnder(targeted, "channelId",i).item(0).getTextContent();
            boolean isDefault = Boolean.parseBoolean(getNodesUnder(targeted, "default", i).item(0).getTextContent());
            targets.add(new Target(serverId, channelId, isDefault));
        }
    }

    private NodeList getNodesUnder(NodeList n, String toSearch, int index){
        return ((Element) n.item(index)).getElementsByTagName(toSearch);
    }

    private Document initialize() throws IOException, SAXException {
        if(!hasConfig()){
            System.out.println("Unable to locate config file. Use dup init to generate one.");
            System.exit(0);
        }
        Document xml = readDocument(getConfig().get());
        System.out.println("Logging in to Discord, please wait...");
        initBot(xml);
        initTargets(xml);
        return xml;
    }

    private void shutdown(Document doc) throws FileNotFoundException, XPathExpressionException {
        if(botInitialized) {
            bot.shutdown();
        }
        doc.getDocumentElement().removeChild(doc.getDocumentElement().getElementsByTagName("targets").item(0));
        //Trim the useless empty space
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
        NodeList emptyTextNodes = (NodeList)
                xpathExp.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < emptyTextNodes.getLength(); i++) {
            Node emptyTextNode = emptyTextNodes.item(i);
            emptyTextNode.getParentNode().removeChild(emptyTextNode);
        }
        //Writes out XML
        Element targetsElement = doc.createElement("targets");
        targets.forEach(t -> t.addToXml(doc, targetsElement));
        doc.getDocumentElement().appendChild(targetsElement);
        writeXml(doc, new FileOutputStream(getConfig().get().toFile()));
    }

    //Commands
    @CommandLine.Command(description = "Initializes dup")
    public void init() throws FileNotFoundException {
        Scanner scan = new Scanner(System.in);
        if(hasConfig()){
            System.out.print("Continuing will delete the previous config file at " + getConfig().get() + "\nContinue? ");
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
        Element targetListElement = doc.createElement("targets");
        if(hasDefault){
            Target t = new Target(serverId, channelId, true);
            t.addToXml(doc, targetListElement);
        }
        root.appendChild(targetListElement);

        writeXml(doc, new FileOutputStream(defaultPath.toFile()));
        pref.put("config-path", defaultPath.toString());
        System.out.println("Setup complete.");
    }

    @CommandLine.Command(name = "add-channel", description = "Interactively adds a channel")
    public void addChannel() throws IOException, SAXException, XPathExpressionException {
        Document doc = initialize();
        Scanner scan = new Scanner(System.in);
        System.out.print("Please enter the server ID: ");
        String serverId = scan.nextLine();
        System.out.print("Please enter the channel ID: ");
        String channelId = scan.nextLine();
        if(bot.getChannel(serverId, channelId).isEmpty()){
            System.out.println("Bot unable to reach channel. Exiting.");
            System.exit(0);
        }
        System.out.print("Set as default? ");
        boolean setDefault = isTrue(scan.nextLine());
        if(setDefault){
            targets.forEach(t -> t.setDefault(false));
        }
        Target target = new Target(serverId, channelId, setDefault);
        if(targets.contains(target)){
            targets.get(targets.indexOf(target)).setDefault(setDefault);
        } else {
            targets.add(target);
        }
        shutdown(doc);
    }

    @CommandLine.Command(description = "Runs as a bot for using slash commands")
    public void daemon() throws IOException, SAXException, XPathExpressionException {
        Document doc = initialize();
        bot.insertSlashCommand(targets.stream().map(Target::guildId).collect(Collectors.toList()));
        //shutdown(doc);
    }

    private static class Target{
        private String guildId;
        private String channelId;
        private boolean isDefault;

        public Target(String guildId, String channelId, boolean isDefault){
            this.guildId = guildId;
            this.channelId = channelId;
            this.isDefault = isDefault;
        }

        public String guildId() {
            return guildId;
        }

        public String channelId() {
            return channelId;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean toSet) {
            isDefault = toSet;
        }
        public Element addToXml(Document doc, Element targetList){
            Element target = doc.createElement("target");
            targetList.appendChild(target);
            Element guildElement = doc.createElement("serverId");
            guildElement.setTextContent(guildId);
            Element channelElement = doc.createElement("channelId");
            channelElement.setTextContent(channelId);
            Element defaultElement = doc.createElement("default");
            defaultElement.setTextContent(Boolean.toString(isDefault));
            target.appendChild(guildElement);
            target.appendChild(channelElement);
            target.appendChild(defaultElement);
            return targetList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;
            return Objects.equals(guildId, target.guildId) && Objects.equals(channelId, target.channelId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(guildId, channelId);
        }

        @Override
        public String toString() {
            return "Target{" +
                    "guildId='" + guildId + '\'' +
                    ", channelId='" + channelId + '\'' +
                    ", isDefault=" + isDefault +
                    '}';
        }
    }
}
