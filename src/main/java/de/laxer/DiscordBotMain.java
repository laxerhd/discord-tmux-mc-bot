package de.laxer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class DiscordBotMain {

    // This should always be the first character when adressing this bot in discord
    private static String prefix = "$";
    private static String status = "mit Fischen";
    
    // This is could be your public IP-Adress or a domain you own
    private static String server_ip = "DEINE_IP_ADRESSE";
    // What is the name of the script used for starting the minecraft server
    private static String server_start_script_name = "./start.sh";

    private static String activation_msg = """
.---.       .-.    _       .-.    .--.       .-.   _             
: .; :     .' `.  :_;     .' `.  : ,. :      : :  :_;            
:   .' .--.`. .'  .-. .--.`. .'  : :: :,-.,-.: :  .-.,-.,-. .--. 
: .; :' .; :: :   : :`._-.': :   : :; :: ,. :: :_ : :: ,. :' '_.'
:___.'`.__.':_;   :_;`.__.':_;   `.__.':_;:_;`.__;:_;:_;:_;`.__.'
            """;


    public static void main(String[] args) throws LoginException {

        // Umgebungsvariable laden
        String token = System.getenv("DISCORD_TOKEN");
        JDABuilder plan = JDABuilder.createDefault(token);

        // Profile setup
        plan.setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing(status))
                .addEventListeners(new MessageReact())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT).build();

        System.out.println(activation_msg);
    }
                                               

    public static String getPrefix() {
        return prefix;
    }

    public static String getServer_ip() {
        return server_ip;
    }

    public static String getServerStartingScriptName(){
        return server_start_script_name;
    }
}

