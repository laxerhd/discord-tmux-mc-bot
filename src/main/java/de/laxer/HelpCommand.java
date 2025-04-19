package de.laxer;

import java.time.Instant;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.slf4j.Logger; // SLF4j Logger

import net.dv8tion.jda.api.EmbedBuilder;

public class HelpCommand implements Command {
    public HelpCommand() {}

    @Override
    public void execute(MessageReceivedEvent event, String message, Logger logger) {
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder description = new StringBuilder("Hier sind alle Befehle, die du verwenden kannst:\n\n");
        MessageChannelUnion channel = event.getChannel();

        Config.commands.forEach((cmd, desc) ->
                description.append("`").append(Config.prefix).append(cmd).append("` - ").append(desc).append("\n")
        );
        // TODO: Utils Classe benutzen, um den Embed zu erstellen
        eb.setTitle("🤖 Bot Hilfe")
                .setColor(0x0099FF) // Helles Blau
                .setDescription(description.toString())
                .setFooter("Angefordert von " + event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now()); // Aktueller Zeitstempel

        channel.sendMessageEmbeds(eb.build()).queue();
    } 
}
