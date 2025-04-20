package de.laxer;

import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;


public class PollCommand extends Command{
    
    public PollCommand(MessageSender messageSender, Logger logger) {
        super(messageSender, logger);
    }
    
    // TODO: MessageSender Klasse benutzen, um den Embed zu erstellen
    @Override
    public void execute(MessageReceivedEvent event, String message) {
        MessageChannelUnion channel = event.getChannel();
        // Originalnachricht löschen (optional, aber oft gewünscht)
        event.getMessage().delete().queue(
                success -> logger.debug("Originalnachricht für Umfrage gelöscht."),
                failure -> logger.warn("Konnte Originalnachricht für Umfrage nicht löschen: {}", failure.getMessage())
        );

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(event.getAuthor().getName() + " fragt:", null, event.getAuthor().getEffectiveAvatarUrl())
                .setColor(0x0099FF)
                .setDescription("**" + message + "**") // Nachricht hervorheben
                .setFooter("Abstimmung gestartet")
                .setTimestamp(Instant.now());

        // Embed senden und Reaktionen hinzufügen
        channel.sendMessageEmbeds(eb.build()).queue(sentMessage -> {
            sentMessage.addReaction(Emoji.fromUnicode("✅")).queue(); // Grüner Haken
            sentMessage.addReaction(Emoji.fromUnicode("❌")).queue(); // Rotes Kreuz
            logger.info("Umfrage '{}' erfolgreich erstellt von {}", message, event.getAuthor().getAsTag());
        }, failure -> logger.error("Konnte Umfrage-Nachricht nicht senden: {}", failure.getMessage(), failure));
    }
}
