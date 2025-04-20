package de.laxer;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;

public class InfoCommand extends Command{

    public InfoCommand(MessageSender messageSender, Logger logger) {
        super(messageSender, logger);
    }

    @Override
    public void execute(MessageReceivedEvent event, String message) {
        MessageChannelUnion channel = event.getChannel();
        messageSender.sendMessageEmbed(channel, event, Status.INFO, "");
    }
}
