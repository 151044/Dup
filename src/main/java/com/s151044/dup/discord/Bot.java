package com.s151044.dup.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.security.auth.login.LoginException;
import java.util.Optional;

public class Bot {
    private String token;
    private String owner;
    private JDA jda;
    public Bot(String token, String owner){
        this.token = token;
        this.owner = owner;
    }
    public boolean login() throws LoginException {
        jda = JDABuilder.createDefault(token).build();
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
}
