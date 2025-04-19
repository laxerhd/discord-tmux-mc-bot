package de.laxer;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger; // SLF4j Logger

public class InfoCommand implements Command{
    
    public void execute(MessageReceivedEvent event, String message, Logger logger) {
        MessageSender messageSender = new MessageSender();
        MessageChannelUnion channel = event.getChannel();
        messageSender.sendMessageEmbed(channel, event, Status.INFO, logger);
    }
}
