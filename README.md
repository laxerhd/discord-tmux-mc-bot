# discord-tmux-mc-bot

## What does this bot do?

This bot is designed for Discord and provides an easy way for friends on a Discord server to control a locally running Minecraft server. The main purpose is to allow restarting the Minecraft server directly through Discord commands.

While the bot can be used to run other scripts and perform tasks on your machine, this usage is **not recommended** and should be done at your own risk. ;D

## How to Set Up?

To set up this bot, you will need a few prerequisites:

1. **A Linux-based system**
   - The bot has been developed and tested on Linux environments, including RaspberryPi OS, so make sure your machine runs a Linux-based operating system.

2. **Java Development Kit (JDK) Version**
   - Download and install the correct JDK version. The bot has been tested with **JDK 17**, though other versions may also work.
   - To install JDK 17, you can use the following command *(Debian-based Systems)*:
     ```sh
     sudo apt-get install openjdk-17-jdk
     ```

3. **Install tmux**
   - Install `tmux` to keep the bot and the Minecraft server running even when you close the terminal.
   - To install `tmux`, use the following command:
     ```sh
     sudo apt-get install tmux
     ```

4. **Discord Bot Token**
   - You will need to add your Discord bot token to your environment variables so that the bot can authenticate with Discord. This can be done by adding the following line to your `.bashrc` or `.bash_profile` file:
     ```sh
     export DISCORD_BOT_TOKEN="your-bot-token-here"
     ```
   - After adding the line, make sure to reload your terminal configuration:
     ```sh
     source ~/.bashrc
     ```

Once these prerequisites are in place, you should be ready to get started!

## Getting Started

If you haven't already set up a Minecraft server, you'll need the necessary files and a bash script to automatically run it on your machine. For guidance on setting up a Minecraft server, just look up a good tutorial online.

### Creating tmux Sessions

To manage both the Discord bot and the Minecraft server, you'll need to create two separate `tmux` sessions. You can do this with the following commands:

- To create a `tmux` session for the Discord bot, run:
  ```sh
  tmux new -s discord-bot
  ```
- To create a `tmux` session for the Minecraft server, run:
  ```sh
  tmux new -s mcserver
  ```

To detach from any `tmux` session, press `Ctrl + B` followed by `D` on your keyboard. This will allow the sessions to continue running in the background.

### Additional Configuration

Lastly, make sure to open the `DiscordBotMain.java` file and update the necessary variables. Specifically, you need to:

- Set your **IP address**.
- Set the **name of your start script** for the Minecraft server.

These values should be correctly filled in to ensure the bot can connect and control the server properly.

### Running the Bot

Once everything is set up, you can start the Discord bot by running the provided `start.bash` script.

1. **Make the Script Executable**

   First, ensure that the `start.bash` script is executable. You can do this by running:
   ```sh
   chmod +x start.bash
   ```

2. **Set Environment Variables**

   Before running the bot, make sure you have set the environment variable for the Discord bot token as described above.
   You can do that by running this command in your discord tmux session.
   ```sh
   export DISCORD_TOKEN="YOUR DISCORD TOKEN"
   ```

3. **Run the Script**

   After making it executable, you can start the bot by running:
   ```sh
   ./start.bash
   ```

This will initialize the bot and make it ready for use on your Discord server. Once running, your friends can use Discord commands to manage the Minecraft server directly, providing a convenient way to control it without needing direct access to the host machine.