package org.example.commands.steamcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SteamUSACommand extends ListenerAdapter {
    private static final int MAX_FIELD_LENGTH = 4096;
    private static final int MAX_DLCS_TO_SHOW = 8;
    private static final String STEAM_URL = "https://store.steampowered.com/app/";

    private JSONObject gameDataCache;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("steamusa")) return;

        OptionMapping gameNameOption = event.getOption("juego");
        if (gameNameOption == null) {
            event.reply("Debes proporcionar el nombre del juego.").queue();
            return;
        }

        String gameName = gameNameOption.getAsString();
        event.deferReply().queue();

        CompletableFuture.supplyAsync(() -> searchGameOnSteam(gameName))
                .thenAccept(gameData -> handleGameData(event, gameData, gameName))
                .exceptionally(ex -> {
                    event.getHook().sendMessage("Error al buscar el juego: " + ex.getMessage()).queue();
                    return null;
                })
                .thenRun(() -> clearCache());
    }

    private void handleGameData(SlashCommandInteractionEvent event, JSONObject gameData, String gameName) {
        if (gameData != null) {
            gameDataCache = gameData;

            EmbedBuilder embed = createGameEmbed(gameData);

            if (gameData.has("dlc")) {
                JSONArray dlcs = gameData.getJSONArray("dlc");
                embed.addField("DLCs", formatDLCInfo(dlcs), false);
            }

            embed.addField("Enlace de compra", STEAM_URL + gameData.getInt("steam_appid") + "/", false);
            embed.setColor(Color.BLUE);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } else {
            suggestSimilarGames(event, gameName);
        }
    }

    private EmbedBuilder createGameEmbed(JSONObject gameData) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(gameData.getString("name"))
                .setDescription(gameData.optString("short_description", "No disponible"))
                .setImage(gameData.optString("header_image", ""));

        addFieldIfExists(embed, "Desarrollador", gameData.opt("developers"));
        addFieldIfExists(embed, "Precio", gameData.opt("price_overview"));
        addFieldIfExists(embed, "Fecha de lanzamiento", gameData.opt("release_date"));
        return embed;
    }

    private String formatDLCInfo(JSONArray dlcs) {
        StringBuilder dlcInfo = new StringBuilder();
        int dlcCount = 0;

        for (int i = 0; i < dlcs.length() && dlcCount < MAX_DLCS_TO_SHOW; i++) {
            int dlcId = dlcs.getInt(i);
            try {
                JSONObject dlcData = getDLCInfo(dlcId);
                String dlcEntry = formatDLCEntry(dlcData, dlcId);
                if (dlcInfo.length() + dlcEntry.length() > MAX_FIELD_LENGTH) {
                    dlcInfo.append("And More Content...");
                    break;
                }
                dlcInfo.append(dlcEntry);
                dlcCount++;
            } catch (Exception e) {
                dlcInfo.append("No se pudo obtener la información del DLC con ID: ").append(dlcId).append("\n");
            }
        }

        return dlcInfo.toString();
    }

    private String formatDLCEntry(JSONObject dlcData, int dlcId) {
        String dlcName = dlcData.getString("name");
        String dlcPrice = dlcData.has("price_overview")
                ? dlcData.getJSONObject("price_overview").getString("final_formatted")
                : "No disponible";
        String dlcLink = STEAM_URL + dlcId + "/";
        return dlcName + " - " + dlcPrice + " [[Comprar](" + dlcLink + ")]\n";
    }

    private void suggestSimilarGames(SlashCommandInteractionEvent event, String gameName) {
        try {
            List<JSONObject> similarGames = searchSimilarGamesOnSteam(gameName);
            if (!similarGames.isEmpty()) {
                StringBuilder suggestions = new StringBuilder("No se encontró el juego. ¿Quizás quisiste decir:\n");
                for (JSONObject similarGame : similarGames) {
                    suggestions.append("- ").append(similarGame.getString("name")).append("\n");
                }
                event.getHook().sendMessage(truncate(suggestions.toString(), MAX_FIELD_LENGTH)).queue();
            } else {
                event.getHook().sendMessage("No se encontró el juego ni juegos similares.").queue();
            }
        } catch (Exception ex) {
            event.getHook().sendMessage("Error al buscar juegos similares: " + ex.getMessage()).queue();
        }
    }

    private void addFieldIfExists(EmbedBuilder embed, String fieldName, Object fieldValue) {
        if (fieldValue == null) return;

        if (fieldValue instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) fieldValue;
            embed.addField(fieldName, truncate(jsonArray.join(", "), MAX_FIELD_LENGTH), true);
        } else if (fieldValue instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) fieldValue;
            if (jsonObject.has("final_formatted")) {
                embed.addField(fieldName, jsonObject.getString("final_formatted"), true);
            } else if (jsonObject.has("date")) {
                embed.addField(fieldName, jsonObject.getString("date"), true);
            }
        } else {
            embed.addField(fieldName, fieldValue.toString(), true);
        }
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private JSONObject searchGameOnSteam(String gameName) {
        try {
            String encodedGameName = URLEncoder.encode(gameName, "UTF-8");
            String apiUrl = "https://store.steampowered.com/api/storesearch/?term=" + encodedGameName + "&l=english&cc=US";
            JSONObject jsonResponse = fetchSteamData(apiUrl);
            JSONArray games = jsonResponse.getJSONArray("items");

            if (games.length() > 0) {
                int appId = games.getJSONObject(0).getInt("id");
                return getGameDetails(appId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al buscar el juego: " + e.getMessage());
        }
        return null;
    }

    private JSONObject fetchSteamData(String apiUrl) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error en la solicitud HTTP: " + con.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return new JSONObject(content.toString());
        }
    }

    private JSONObject getGameDetails(int appId) throws Exception {
        String apiUrl = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&cc=US";
        JSONObject json = fetchSteamData(apiUrl);
        return json.getJSONObject(String.valueOf(appId)).getJSONObject("data");
    }

    private JSONObject getDLCInfo(int dlcId) throws Exception {
        String apiUrl = "https://store.steampowered.com/api/appdetails?appids=" + dlcId;
        JSONObject json = fetchSteamData(apiUrl);
        return json.getJSONObject(String.valueOf(dlcId)).getJSONObject("data");
    }

    private List<JSONObject> searchSimilarGamesOnSteam(String gameName) throws Exception {
        return new ArrayList<>();
    }

    private void clearCache() {
        gameDataCache = null;
    }
}