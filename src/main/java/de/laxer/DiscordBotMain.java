package de.laxer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger; // SLF4j Logger
import org.slf4j.LoggerFactory; // SLF4j Factory

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DiscordBotMain {

    // SLF4j Logger Instanz holen
    private static final Logger logger = LoggerFactory.getLogger(DiscordBotMain.class);

    // --- Konfiguration ---
    private static final String prefix = "$";
    private static final String status = "mit Fischen";
    private static final String server_ip = "friendsplaytogether.de"; // Oder aus Config laden
    private static final String server_start_script_name = "start2.sh"; // Sicherstellen, dass Pfad korrekt ist!
    private static final String tmux_session_name = "mcserver";

    // ExecutorService für asynchrone Aufgaben
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final String activation_msg = """
    .---.       .-.    _       .-.    .--.       .-.   _
    : .; :     .' `.  :_;     .' `.  : ,. :      : :  :_;
    :   .' .--.`. .'  .-. .--.`. .'  : :: :,-.,-.: :  .-.,-.,-. .--.
    : .; :' .; :: :   : :`._-.': :   : :; :: ,. :: :_ : :: ,. :' '_.'
    :___.'`.__.':_;   :_;`.__.':_;   `.__.':_;:_;`.__;:_;:_;:_;`.__.'
                """;


    public static void main(String[] args) {

        // Umgebungsvariable laden
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) { // isBlank() prüft auf null, leer und nur Whitespace
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            logger.error("FEHLER: DISCORD_TOKEN Umgebungsvariable nicht gesetzt oder leer!");
            logger.error("Bot kann nicht starten.");
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.exit(1);
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token);

            // Profile setup
            builder.setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing(status))
                    // Übergebe den ExecutorService an den Listener
                    .addEventListeners(new MessageReact(executorService))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    // Deaktiviere unnötige Caches (SCHEDULED_EVENTS ist in JDA 5 stabil vorhanden)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS);

            // Baue JDA Instanz
            JDA jda = builder.build();

            // Warte bis JDA vollständig initialisiert ist
            jda.awaitReady();

            logger.info(activation_msg); // Logge die Aktivierungsnachricht
            logger.info("====================================================");
            logger.info("Bot ist online und bereit!");
            logger.info("Prefix: {}", prefix);
            logger.info("Minecraft Server IP: {}", server_ip);
            logger.info("Start Script: {}", server_start_script_name);
            logger.info("TMUX Session: {}", tmux_session_name);
            logger.info("====================================================");


            // Graceful Shutdown Hook hinzufügen
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Fahre Bot und ExecutorService herunter...");
                jda.shutdown(); // Fährt JDA herunter
                executorService.shutdown(); // Initiiert das Herunterfahren des Thread Pools
                try {
                    // Warte eine kurze Zeit auf die Beendigung laufender Tasks
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warn("ExecutorService wurde nicht innerhalb von 5s beendet, erzwinge Abbruch.");
                        executorService.shutdownNow(); // Erzwingt das Herunterfahren
                    }
                } catch (InterruptedException e) {
                    logger.error("Warten auf ExecutorService-Shutdown unterbrochen.", e);
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt(); // Setze Interrupt-Status erneut
                }
                logger.info("Bot heruntergefahren.");
            }));

        } catch (InterruptedException e) {
            logger.error("Warten auf JDA Bereitschaft wurde unterbrochen!", e);
            Thread.currentThread().interrupt(); // Wichtig: Interrupt-Status wieder setzen
        } catch (Exception e) { // Fange generische Exceptions beim Start ab
            logger.error("Ein unerwarteter Fehler ist beim Starten des Bots aufgetreten!", e);
            System.exit(1);
        }
    }


    // --- Getter für Konfiguration ---
    public static String getPrefix() {
        return prefix;
    }

    public static String getServer_ip() {
        return server_ip;
    }

    public static String getServerStartingScriptName(){
        return server_start_script_name;
    }

    public static String getTmuxSessionName() {
        return tmux_session_name;
    }
}
