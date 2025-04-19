package de.laxer;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger; // SLF4j Logger


public interface Command {
    void execute(MessageReceivedEvent event, String message, Logger logger);
}
