package org.example.commands.steamcommands.steamworld;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SteamAmericaCommand extends ListenerAdapter {
    private static final String STEAM_API_URL_SEARCH = "https://store.steampowered.com/api/storesearch/?term=";
    private static final String STEAM_API_URL_DETAILS = "https://store.steampowered.com/api/appdetails?appids=";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("steam_america")) return;

        event.deferReply().queue();

        OptionMapping gameOption = event.getOption("juego");
        OptionMapping regionOption = event.getOption("region");

        if (gameOption == null) {
            event.reply("Por favor, proporciona el nombre de un juego.").setEphemeral(true).queue();
            return;
        }

        String gameName = gameOption.getAsString();
        String region = regionOption != null ? regionOption.getAsString().toUpperCase() : "US"; // Por defecto, región 'US'

        try {
            JSONObject gameData = searchGameOnSteam(gameName, region);
            if (gameData != null) {
                sendGameDetails(event, gameData, region);
            } else {
                event.getHook().sendMessage("No se encontró el juego en la región especificada.").queue();
            }
        } catch (Exception e) {
            event.getHook().sendMessage("Ocurrió un error: " + e.getMessage()).queue();
        }
    }

    private JSONObject searchGameOnSteam(String gameName, String region) throws Exception {
        String queryUrl = STEAM_API_URL_SEARCH + URLEncoder.encode(gameName, "UTF-8") + "&cc=" + region;
        String response = sendHttpRequest(queryUrl);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray games = jsonResponse.optJSONArray("items");

        if (games != null && games.length() > 0) {
            JSONObject game = games.getJSONObject(0); // Tomar el primer resultado
            int appId = game.getInt("id");
            return fetchGameDetails(appId, region);
        }

        return null; // No se encontraron juegos
    }

    private JSONObject fetchGameDetails(int appId, String region) throws Exception {
        String queryUrl = STEAM_API_URL_DETAILS + appId + "&cc=" + region;
        String response = sendHttpRequest(queryUrl);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.optJSONObject(String.valueOf(appId)).optJSONObject("data");
    }

    private String sendHttpRequest(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void sendGameDetails(SlashCommandInteractionEvent event, JSONObject gameData, String region) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(gameData.getString("name"))
                .setDescription(gameData.optString("short_description", "Descripción no disponible"))
                .setImage(gameData.optString("header_image", ""))
                .setColor(Color.BLUE);

        if (gameData.has("price_overview")) {
            JSONObject priceOverview = gameData.getJSONObject("price_overview");
            double finalPrice = priceOverview.getDouble("final") / 100.0;
            double initialPrice = priceOverview.getDouble("initial") / 100.0;
            int discountPercent = priceOverview.optInt("discount_percent", 0);

            String priceText = discountPercent > 0
                    ? String.format("~~%.2f~~ → %.2f (%d%% de descuento)", initialPrice, finalPrice, discountPercent)
                    : String.format("%.2f", finalPrice);

            embed.addField("💵 Precio", priceText + " " + priceOverview.getString("currency"), false);
        } else if (gameData.optBoolean("is_free", false)) {
            embed.addField("💵 Precio", "🎉 Gratis", false);
        }

        embed.addField("🌍 Región", region, true)
                .addField("🔗 Comprar en Steam", "[Haz clic aquí](https://store.steampowered.com/app/" + gameData.getInt("steam_appid") + ")", false);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
