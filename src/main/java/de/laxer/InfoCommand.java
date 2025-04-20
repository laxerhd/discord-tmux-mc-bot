package de.laxer;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;

public class InfoCommand implements Command{
    private final MessageSender messageSender;
    private final Logger logger;

    public InfoCommand(MessageSender messageSender, Logger logger) {
        this.messageSender = messageSender;
        this.logger = logger;   
    }

    @Override
    public void execute(MessageReceivedEvent event, String message) {
        MessageChannelUnion channel = event.getChannel();
        messageSender.sendMessageEmbed(channel, event, Status.INFO);
    }
}
