package org.example.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;

public class CreditsCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("creditos")) {
            event.deferReply().queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🔍 Comandos Disponibles");
            embed.setColor(Color.BLACK);

            embed.addField("💎 Creador.", "Bot `Matecito 🧉` creado por MtrVoid (mtrvoid en Discord) y publicado por el mismo.", false);
            embed.addField("⛽ Host.", "Bot hosteado en `VexyHost.com`.", false);
            embed.addField("☕ Primeros Donadores", "Muchísimas gracias a las siguientes personas:\n1. Shuriken \n2. Moshi\n3. Chanclas\n4. TheTomHat\n5. Raamii", false);

            Button vexyHostButton = Button.link(
                    "https://vexyhost.com/es-ar/discord-hosting",
                    "「⛽」 VexyHost.com/discord-hosting."
            );

            Button cafecitoButton = Button.link(
                    "https://cafecito.app/matecitobotdiscord",
                    "「☕」 Apóyame con un cafecito"
            );

            Button mtrVoidTwitterButton = Button.link(
                    "https://x.com/MtrVoid",
                    "「🐦」 Twitter de MtrVoid."
            );

            Button supportServerButton = Button.link(
                    "https://discord.gg/dD3J9X8gBs",
                    "「🔎」 Servidor de actualizaciones y soporte."
            );

            embed.setImage("https://private-user-images.githubusercontent.com/173469792/371767315-85f17ac2-82a2-407a-9a40-6f3adf82c949.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3Mjc1Mzc5OTIsIm5iZiI6MTcyNzUzNzY5MiwicGF0aCI6Ii8xNzM0Njk3OTIvMzcxNzY3MzE1LTg1ZjE3YWMyLTgyYTItNDA3YS05YTQwLTZmM2FkZjgyYzk0OS5wbmc_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1BS0lBVkNPRFlMU0E1M1BRSzRaQSUyRjIwMjQwOTI4JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI0MDkyOFQxNTM0NTJaJlgtQW16LUV4cGlyZXM9MzAwJlgtQW16LVNpZ25hdHVyZT1jOGY0YzQ3NDViMzhjNmJjMDU4MDkwNjY1NWQ4MTg5ZDNkYzk1ZGMxNjc3ZjAzMTdmMDc1Mzg1MDU1YzhlMTk4JlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCJ9.HnCbQvHDo09XCp6sG07ly9eJsDf8jgopeE3LURWlTwM");

            event.getHook().sendMessageEmbeds(embed.build())
                    .addActionRow(mtrVoidTwitterButton, cafecitoButton, vexyHostButton, supportServerButton)
                    .queue();
        }
    }
}

