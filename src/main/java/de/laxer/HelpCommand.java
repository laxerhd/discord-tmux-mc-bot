package de.laxer;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.slf4j.Logger;

public class HelpCommand extends Command {

    public HelpCommand(MessageSender messageSender, Logger logger) {
        super(messageSender, logger);
    }

    @Override
    public void execute(MessageReceivedEvent event, String message) {
        MessageChannelUnion channel = event.getChannel();
        messageSender.sendMessageEmbed(channel, event, Status.HELP, "");
    }
}
