#!/bin/bash

mvn compile
echo "Starte den Bot..."
mvn exec:java -Dexec.mainClass="de.laxer.DiscordBotMain"
