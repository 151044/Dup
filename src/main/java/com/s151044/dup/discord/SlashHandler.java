package com.s151044.dup.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SlashHandler extends ListenerAdapter {

    private String owner;

    public SlashHandler(String owner){
        this.owner = owner;
    }
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getName().equals("dup-up")) {
            if (event.getUser().getId().equals(owner)) {
                ReplyCallbackAction callback = event.deferReply();
                File f = new File(event.getOption("path").getAsString());
                if(f.exists()) {
                    callback.addFile(f).queue();
                } else {
                    callback.setContent("Failed to find file!").queue();
                }
            } else {
                event.reply("You're not authorized to use this function!").queue();
            }
        }
    }
}
