package com.s151044.dup.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Optional;

public class Bot {
    private String token;
    private String owner;
    private CommandData command;
    private JDA jda;
    public Bot(String token, String owner){
        this.token = token;
        this.owner = owner;
        command = Commands.slash("dup-up","Uploads a file from the local file system.")
                .addOption(OptionType.STRING, "path", "The path to the file",true);
    }
    public boolean login() throws LoginException {
        jda = JDABuilder.createDefault(token).addEventListeners(new SlashHandler(owner)).build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }
    public Optional<TextChannel> getChannel(String guildId, String channelId){
        Guild g = jda.getGuildById(guildId);
        if(g == null){
            return Optional.empty();
        }
        return Optional.ofNullable(g.getTextChannelById(channelId));
    }
    public Optional<Guild> getGuild(String guildId){
        return Optional.ofNullable(jda.getGuildById(guildId));
    }
    public void shutdown(){
        jda.shutdown();
    }
    public void insertSlashCommand(List<String> guildIds){
        guildIds.forEach((str) -> {
            Optional<Guild> guild = getGuild(str);
            if(guild.isEmpty()){
                return;
            }
            guild.get().upsertCommand(command).queue();
        });
    }
}
