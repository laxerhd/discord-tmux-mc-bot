package de.laxer;

import org.slf4j.Logger;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
    protected final MessageSender messageSender;
    protected final Logger logger;

    public Command(MessageSender messageSender, Logger logger) {
        this.messageSender = messageSender;
        this.logger = logger;
    }

    abstract void execute(MessageReceivedEvent event, String message);

}
