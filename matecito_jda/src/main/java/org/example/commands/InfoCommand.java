package org.example.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;

public class InfoCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("info")) {
            event.deferReply().queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🔍 Comandos Disponibles");
            embed.setColor(Color.BLACK);

            embed.addField("/creditos", "📨 Muestra los creditos del bot.", false);
            embed.addField("/convert", "💸 Es un comando que sirve para convertir de una moneda *A* el valor a una moneda *B*. También posee la opción de hacerlo por cantidades.", false);
            embed.addField("/dolarhoy", "🔥 Es un comando que muestra los distintos tipos de dólar y el valor compra/venta.)", false);
            embed.addField("/dollarsto_ars", "💸➡️🪗 Es un comando que convierte el los d+olares a pesos de distintos tipos de cambio.)", false);
            embed.addField("/pesosto_usd", "🪗➡️💸 Es un comando que convierte el los pesos a dólares de distintos tipos de cambio.)", false);
            embed.addField("/heavycalculator", "➕ Es un comando que sirve como calculadora científica. Usar con cuidado, una real siempre será mejor.", false);
            embed.addField("/steamcito (El preferido de papá)", "🧉 Es un comando que te muestra el precio de los juegos en pesos argentinos (ARS) de la plataforma Steam. Estos no incluyen el impuesto provincial.", false);
            embed.addField("/steamusuario", "📷 Es un comando que te muestra el perfil de un usuario de Steam usando su ID. (Los números al final del link de perfil)", false);
            embed.addField("/steamusa", "💲 Es un comando que te muestra el precio de cualquier juego de Steam en precio estadounidense.", false);

            Button cafecitoButton = Button.link(
                    "https://cafecito.app/matecitobotdiscord",
                    "「☕」 Apóyame con un cafecito"
            );

            Button matecitoBotButton = Button.link(
                    "https://github.com/MtrVoid/matecito_services",
                    "「🤖」 Agregua el bot a tu servidor."
            );

            Button steamcitoButton = Button.link(
                    "https://steamcito.com.ar/",
                    "「♨️」 Ve a Steamcito Oficial"
            );

            Button gitButton = Button.link(
                    "https://github.com/MtrVoid/matecito_services",
                    "「📖」 ToS y PoP"
            );

            event.getHook().sendMessageEmbeds(embed.build())
                    .addActionRow(cafecitoButton)
                    .addActionRow(matecitoBotButton)
                    .addActionRow(steamcitoButton)
                    .addActionRow(gitButton)
                    .queue();
        }
    }
}
