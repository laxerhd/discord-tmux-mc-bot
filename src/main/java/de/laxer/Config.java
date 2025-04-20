package de.laxer;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public static final String prefix = "$";
    public static final String status = "mit Fischen";
    public static final String server_ip = "friendsplaytogether.de"; // Oder aus Config laden
    public static final String server_start_script_name = "start2.sh"; // Sicherstellen, dass Pfad korrekt ist!
    public static final String tmux_session_name = "mcserver";

    // Befehlsliste
    public static final Map<String, String> commands = new HashMap<>() {
        {
            put("help", "Zeigt alle verfügbaren Befehle an.");
            put("e", "Erstellt eine Umfrage mit ✅ / ❌ Reaktionen.");
            put("info", "Zeigt Informationen über den Bot an.");
            put("restart", "Startet den Minecraft-Server (nur wenn er offline ist).");
            put("status", "Überprüft, ob der Minecraft-Server online oder offline ist.");
        }
    };
}
