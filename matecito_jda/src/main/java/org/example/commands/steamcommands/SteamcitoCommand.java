package org.example.commands.steamcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.example.utilities.CurrencyConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jsoup.Jsoup;
import org.example.utilities.ErrorMessages;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SteamcitoCommand extends ListenerAdapter {
    private static final int MAX_FIELD_LENGTH = 4096;
    private static final int MAX_DLCS_TO_SHOW = 32;

    private static final String STEAM_API_URL_PART1_INIT = "https://store.steampowered.com/api/appdetails?appids=";
    private static final String STEAM_API_URL_PART2_LANGUAGE = "&l=";
    private static final String STEAM_API_URL_PART3_REGION = "&cc=";

    private static final String STEAM_API_URL_SEARCHGAME_PART1_INIT = "https://store.steampowered.com/api/storesearch/?term=";
    private static final String STEAM_API_URL_SEARCHGAME_PART2_LANGUAGE_AND_REGION = "&l=english&cc=AR";

    private static final String STEAM_API_URL_SEARCHGAME_REGION_AR = "&cc=AR";

    private String cleanHtml(String html) {
        return Jsoup.parse(html).text();
    }

    public SteamcitoCommand() {
        // Constructor vacío porque ya no se utiliza ServersData.
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            if (event.getName().equals("steamcito")) {
                event.deferReply().queue();

                OptionMapping showContent = event.getOption("mostrar");
                OptionMapping conversionTypeOption = event.getOption("conversion");
                OptionMapping gameNameOption = event.getOption("juego");
                OptionMapping zoneTaxesOption = event.getOption("impuestoprovincial");

                if (gameNameOption == null) {
                    event.reply("Debes proporcionar el nombre del juego.").setEphemeral(true).queue();
                    return;
                }

                String showContentType = showContent != null ? showContent.getAsString() : "completo";
                String gameName = normalizeGameName(gameNameOption.getAsString());
                double zoneTaxes = zoneTaxesOption != null ? zoneTaxesOption.getAsDouble() : 0;
                String conversionType = conversionTypeOption != null ? conversionTypeOption.getAsString() : "Dólar Tarjeta";

                CompletableFuture.supplyAsync(() -> {
                            try {
                                return searchGameOnSteam(gameName);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .thenAccept(gameData -> {
                            try {
                                if ("mostrar_concreto".equals(showContentType)) {
                                    handleConcreteGameData(event, gameData, conversionType, zoneTaxes);
                                } else {
                                    handleGameData(event, gameData, conversionType, zoneTaxes);
                                }
                            } finally {
                                gameData.clear();
                            }
                        })
                        .exceptionally(ex -> handleError(event, ex));
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private Void handleError(SlashCommandInteractionEvent event, Throwable ex) {
        event.getHook().sendMessage("Error al buscar el juego: " + ex.getMessage()).queue();
        ex.printStackTrace();
        return null;
    }

    private void handleGameData(SlashCommandInteractionEvent event, JSONObject gameData, String conversionType, double zoneTaxes) {
        if (gameData == null) {
            event.getHook().sendMessage("No se encontró el juego. Por favor, revisa el nombre y vuelve a intentarlo.").queue();
            return;
        }

        EmbedBuilder gameEmbed = createExtendedGameEmbed(gameData, conversionType, zoneTaxes);
        event.getHook().sendMessageEmbeds(gameEmbed.build()).queue();

        if (gameData.has("dlc")) {
            sendDLCs(event, gameData, conversionType, zoneTaxes);
        }

        sendSystemRequirements(event, gameData);
    }

    private void handleConcreteGameData(SlashCommandInteractionEvent event, JSONObject gameData, String conversionType, double zoneTaxes) {
        if (gameData == null) {
            event.getHook().sendMessage("No se encontró el juego. Por favor, revisa el nombre y vuelve a intentarlo.").queue();
            return;
        }

        EmbedBuilder gameEmbed = createConcreteGameEmbed(gameData, conversionType, zoneTaxes);
        event.getHook().sendMessageEmbeds(gameEmbed.build()).queue();

        if (gameData.has("dlc")) {
            sendDLCs(event, gameData, conversionType, zoneTaxes);
        }
    }

    // Debajo


    private EmbedBuilder createConcreteGameEmbed(JSONObject gameData, String conversionType, double zoneTaxes) {
        EmbedBuilder gameEmbed = new EmbedBuilder();
        gameEmbed.setTitle(gameData.getString("name"))
                .setDescription(gameData.optString("short_description", "No disponible"))
                .setImage(gameData.optString("header_image", ""))
                .setColor(Color.BLUE);

        if (gameData.has("metacritic")) {
            JSONObject metacritic = gameData.getJSONObject("metacritic");
            gameEmbed.addField("📊 Metacritic Score", metacritic.getInt("score") + "/100 [[Ver más]( " + metacritic.getString("url") + " )]", true);
        }

        addRatings(gameEmbed, gameData);

        handlePriceInformation(gameEmbed, gameData, gameData.optInt("steam_appid"), conversionType, zoneTaxes);

        return gameEmbed;
    }

    private EmbedBuilder createExtendedGameEmbed(JSONObject gameData, String conversionType, double zoneTaxes) {
        EmbedBuilder gameEmbed = new EmbedBuilder();
        gameEmbed.setTitle(gameData.getString("name"))
                .setDescription(gameData.optString("short_description", "No disponible"))
                .setImage(gameData.optString("header_image", ""))
                .setThumbnail("https://media.discordapp.net/attachments/815395979585912842/1285652974772949068/icon128.png");

        addBasicInfo(gameEmbed, gameData);
        addRatings(gameEmbed, gameData);
        handlePriceInformation(gameEmbed, gameData, gameData.optInt("steam_appid"), conversionType, zoneTaxes);

        return gameEmbed;
    }

    private void addBasicInfo(EmbedBuilder gameEmbed, JSONObject gameData) {
        gameEmbed.addField("💾 Desarrollador", extractArrayAsString(gameData.optJSONArray("developers")), true)
                .addField("🗿 Distribuidores", extractArrayAsString(gameData.optJSONArray("publishers")), true)
                .addField("📅 Fecha de lanzamiento", gameData.optJSONObject("release_date").optString("date", "No disponible"), true)
                .addField("📚 Idiomas soportados", cleanHtml(gameData.optString("supported_languages", "No disponible")), true)
                .addField("🎮 Categorías", extractDescriptions(gameData.optJSONArray("categories")), true)
                .addField("💻 Plataformas", extractPlatforms(gameData.optJSONObject("platforms")), false)
                .addField("🎨 Géneros", extractDescriptions(gameData.optJSONArray("genres")), true)
                .addField("🏆 Logros", gameData.has("achievements") ? String.valueOf(gameData.getJSONObject("achievements").optInt("total")) : "No disponible", true)
                .addField("📦 Tipo", gameData.optString("type", "No disponible"), false);

        if (gameData.has("metacritic")) {
            JSONObject metacritic = gameData.getJSONObject("metacritic");
            gameEmbed.addField("📊 Metacritic Score", metacritic.getInt("score") + "/100 [[Ver más]( " + metacritic.getString("url") + " )]", true);
        }
    }

    private void addRatings(EmbedBuilder gameEmbed, JSONObject gameData) {
        if (!gameData.has("ratings")) return;

        JSONObject ratings = gameData.getJSONObject("ratings");
        addRatingField(gameEmbed, "ESRB", ratings.optJSONObject("esrb"), "rating", "descriptors");
        addRatingField(gameEmbed, "USK", ratings.optJSONObject("usk"), "rating", null);
    }

    private void addRatingField(EmbedBuilder gameEmbed, String ratingName, JSONObject ratingData, String ratingKey, String descriptorsKey) {
        if (ratingData == null) return;

        String rating = ratingData.optString(ratingKey, "No disponible");
        String descriptors = descriptorsKey != null ? ratingData.optString(descriptorsKey, "No disponible") : "";

        gameEmbed.addField("🎮 " + ratingName + " Rating", rating + (descriptors.isEmpty() ? "" : "\n" + descriptors), false);
    }


    private void handlePriceInformation(EmbedBuilder gameEmbed, JSONObject gameData, int appId, String conversionType, double zoneTaxes) {
        String purchaseLink = "https://store.steampowered.com/app/" + appId + "/";
        String tipoDolar = "";
        String DOLAR_TYPE_USED = "";
        String EMOTE_ICON_TYPE_USED = "";

        if (gameData.has("is_free") && gameData.getBoolean("is_free")) {
            gameEmbed.addField("📰 Precio único:", "🎉 Gratis", true);
        } else if (gameData.has("price_overview")) {
            JSONObject priceOverview = gameData.getJSONObject("price_overview");
            double priceUSD = priceOverview.optDouble("final", 0) / 100;
            double initialPriceUSD = priceOverview.optDouble("initial", 0) / 100;
            int discountPercent = priceOverview.optInt("discount_percent", 0);

            String priceDisplayUSD = generatePriceDisplayUSD(priceUSD, initialPriceUSD, discountPercent);
            gameEmbed.addField("💵 Precio en USD", priceDisplayUSD, true);

            double priceARS;

            if ("oficial".equals(conversionType)) {
                priceARS = CurrencyConverter.convertToARS(priceUSD, conversionType);
                DOLAR_TYPE_USED = "Dólar Bancario";
                EMOTE_ICON_TYPE_USED = "🏦";
            } else if ("cripto".equals(conversionType)) {
                priceARS = CurrencyConverter.convertToARS(priceUSD, conversionType);
                DOLAR_TYPE_USED = "Cripto";
                EMOTE_ICON_TYPE_USED = "🪙";
            } else {
                priceARS = CurrencyConverter.convertToARS(priceUSD, conversionType);
                DOLAR_TYPE_USED = "Dólar Tarjeta";
                EMOTE_ICON_TYPE_USED = "💳";
            }

            double priceWithTaxes = priceARS * (1 + zoneTaxes / 100);

            String priceDisplayARS;
            if (discountPercent > 0) {
                double originalPriceARS = priceARS / (1 - discountPercent / 100.0);
                originalPriceARS *= (1 + zoneTaxes / 100);
                priceDisplayARS = String.format("~~ARS %.2f~~ → " + EMOTE_ICON_TYPE_USED + "ARS %.2f (-%d%%)", originalPriceARS, priceWithTaxes, discountPercent);
            } else {
                priceDisplayARS = String.format("ARS %.2f", priceWithTaxes);
            }

            gameEmbed.addField("🧉 Precio en ARS (" + DOLAR_TYPE_USED + ")", EMOTE_ICON_TYPE_USED +  priceDisplayARS, true);
        } else {
            gameEmbed.addField("📰 Precio único:", "🎉 Gratis", true);
        }

        gameEmbed.addField("🛒 Comprar en Steam", "[🔗 Haz clic aquí](" + purchaseLink + ")", false);
    }

    private String generatePriceDisplayUSD(double priceUSD, double initialPriceUSD, int discountPercent) {
        if (discountPercent == 100) {
            return String.format("🎉 Gratis (Precio original: USD %.2f, -%d%%)", initialPriceUSD, discountPercent);
        } else if (discountPercent > 0) {
            return String.format("~~USD %.2f~~ → USD %.2f (-%d%%)", initialPriceUSD, priceUSD, discountPercent);
        } else {
            return String.format("USD %.2f", priceUSD);
        }
    }

    private void sendDLCs(SlashCommandInteractionEvent event, JSONObject gameData, String conversionType, double zoneTaxes) {
        Message loadingMessage = event.getHook().sendMessage("# ⏱️ Cargando DLCs y Especificaciones...").complete();
        EmbedBuilder dlcEmbed = new EmbedBuilder();
        dlcEmbed.setTitle("Contenido adicional (DLCs)");

        JSONArray dlcs = gameData.getJSONArray("dlc");
        StringBuilder dlcInfo = new StringBuilder();
        int dlcCount = 0;

        for (int i = 0; i < dlcs.length() && dlcCount < MAX_DLCS_TO_SHOW; i++) {
            int dlcId = dlcs.getInt(i);
            try {
                JSONObject dlcData = getDLCInfo(dlcId);
                String dlcEntry = formatDLCEntry(dlcData, conversionType, zoneTaxes);

                if (dlcInfo.length() + dlcEntry.length() > MAX_FIELD_LENGTH) {
                    break;
                }

                dlcInfo.append(dlcEntry);
                dlcCount++;
            } catch (Exception e) {
                dlcInfo.append("No se pudo obtener la información del DLC con ID: ").append(dlcId).append("\n");
            }
        }

        if (dlcCount == 0) {
            dlcInfo.append("No hay DLCs disponibles.");
        }

        dlcEmbed.setDescription(dlcInfo.length() > 0 ? dlcInfo.toString() : "No hay DLCs disponibles.");
        dlcEmbed.setColor(Color.BLUE);
        event.getHook().sendMessageEmbeds(dlcEmbed.build()).queue(response -> loadingMessage.delete().queue());
    }

    private String formatDLCEntry(JSONObject dlcData, String conversionType, double zoneTaxes) {
        String dlcName = dlcData.getString("name");
        String EMOTE_ICON_TYPE_USED = getEmoteIconForDollarType(conversionType);

        JSONObject priceOverview = dlcData.optJSONObject("price_overview");
        double dlcPriceUSD = priceOverview != null ? priceOverview.optDouble("final", 0) / 100 : 0;
        double initialDlcPriceUSD = priceOverview != null ? priceOverview.optDouble("initial", 0) / 100 : 0;
        int dlcDiscountPercent = priceOverview != null ? priceOverview.optInt("discount_percent", 0) : 0;

        double dlcPriceARS = CurrencyConverter.convertToARS(dlcPriceUSD, conversionType);

        if (dlcPriceARS != -1) {
            dlcPriceARS *= (1 + zoneTaxes / 100);
        }

        StringBuilder entryBuilder = new StringBuilder();
        entryBuilder.append(dlcName).append(" - ");

        if (dlcPriceUSD == 0 || dlcDiscountPercent == 100) {
            entryBuilder.append(String.format("🎉 Gratis [[Comprar](%s)]\n", "https://store.steampowered.com/app/" + dlcData.getInt("steam_appid") + "/"));
        } else if (dlcDiscountPercent > 0) {
            double originalPriceARS = CurrencyConverter.convertToARS(initialDlcPriceUSD, conversionType);
            if (originalPriceARS != -1) {
                originalPriceARS *= (1 + zoneTaxes / 100);
            }
            entryBuilder.append(String.format("~~USD %.2f~~ → USD %.2f (-%d%%) / ", initialDlcPriceUSD, dlcPriceUSD, dlcDiscountPercent));
            entryBuilder.append(String.format("~~ARS %.2f~~ → " + EMOTE_ICON_TYPE_USED + " ARS %.2f [[Comprar](%s)]\n", originalPriceARS, dlcPriceARS, "https://store.steampowered.com/app/" + dlcData.getInt("steam_appid") + "/"));
        } else {
            entryBuilder.append(String.format("USD %.2f / " + EMOTE_ICON_TYPE_USED + "ARS %.2f [[Comprar](%s)]\n", dlcPriceUSD, dlcPriceARS, "https://store.steampowered.com/app/" + dlcData.getInt("steam_appid") + "/"));
        }

        return entryBuilder.toString();
    }

    private String getEmoteIconForDollarType(String conversionType) {
        return switch (conversionType.toLowerCase()) {
            case "cripto" -> "🪙";
            case "tarjeta" -> "💳";
            default -> "🏦";
        };
    }


    private void sendSystemRequirements(SlashCommandInteractionEvent event, JSONObject gameData) {
        EmbedBuilder requirementsEmbed = new EmbedBuilder();
        requirementsEmbed.setTitle("⚙️ Requerimientos del Sistema");
        requirementsEmbed.setColor(Color.ORANGE);

        if (gameData.has("pc_requirements") && gameData.get("pc_requirements") instanceof JSONObject) {
            JSONObject requirementsPC = gameData.getJSONObject("pc_requirements");
            String minimumReqs = cleanHtml(requirementsPC.optString("minimum", "No disponible"));
            String recommendedReqs = cleanHtml(requirementsPC.optString("recommended", "No disponible"));
            requirementsEmbed.addField("💻 PC - Mínimos", minimumReqs, false);
            requirementsEmbed.addField("💻 PC - Recomendados", recommendedReqs, false);
        }

        if (gameData.has("mac_requirements") && gameData.get("mac_requirements") instanceof JSONObject) {
            JSONObject requirementsMac = gameData.getJSONObject("mac_requirements");
            String minimumReqs = cleanHtml(requirementsMac.optString("minimum", "No disponible"));
            String recommendedReqs = cleanHtml(requirementsMac.optString("recommended", "No disponible"));
            requirementsEmbed.addField("🍏 Mac - Mínimos", minimumReqs, false);
            requirementsEmbed.addField("🍏 Mac - Recomendados", recommendedReqs, false);
        }

        if (gameData.has("linux_requirements") && gameData.get("linux_requirements") instanceof JSONObject) {
            JSONObject requirementsLinux = gameData.getJSONObject("linux_requirements");
            String minimumReqs = cleanHtml(requirementsLinux.optString("minimum", "No disponible"));
            String recommendedReqs = cleanHtml(requirementsLinux.optString("recommended", "No disponible"));
            requirementsEmbed.addField("🐧 Linux - Mínimos", minimumReqs, false);
            requirementsEmbed.addField("🐧 Linux - Recomendados", recommendedReqs, false);
        }

        event.getHook().sendMessageEmbeds(requirementsEmbed.build()).queue();
    }

    private JSONObject fetchGameDetails(int appId, String language, String region) throws Exception {
        try {
            String apiUrl = STEAM_API_URL_PART1_INIT + appId + STEAM_API_URL_PART2_LANGUAGE + language + STEAM_API_URL_PART3_REGION + region;
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(ErrorMessages.HTTP_ERROR_MESSAGE + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONObject json = new JSONObject(content.toString());
            JSONObject gameData = json.optJSONObject(String.valueOf(appId)).optJSONObject("data");
            if (gameData != null) {
                return gameData;
            }
            return null;
        } catch (IOException | JSONException e) {
            System.err.println(ErrorMessages.GAME_DETAILS_ERROR_MESSAGE + e.getMessage());
            throw new Exception(ErrorMessages.GAME_DETAILS_RETRY_ERROR_MESSAGE);
        }
    }

    private JSONObject getGameDetails(int appId) throws Exception {
        JSONObject gameData = fetchGameDetails(appId, "spanish", "AR");

        if (gameData == null || !hasValidPrice(gameData)) {
            gameData = fetchGameDetails(appId, "english", "US");
        }

        if (gameData == null) {
            throw new Exception(ErrorMessages.DONT_FOUND_GAME_DETAILS_ERROR_MESSAGE);
        }

        return gameData;
    }

    private boolean hasValidPrice(JSONObject gameData) {
        if (gameData == null) {
            return false;
        }

        JSONObject priceOverview = gameData.optJSONObject("price_overview");
        if (priceOverview == null) {
            return false;
        }

        int finalPrice = priceOverview.optInt("final", -1);

        return finalPrice > 0;
    }

    private String extractArrayAsString(JSONArray array) {
        return array != null
                ? array.toList().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "))
                : "No disponible";
    }

    private String extractDescriptions(JSONArray array) {
        if (array == null) return "No disponible";

        return array.toList().stream()
                .map(item -> {
                    if (item instanceof HashMap) {
                        JSONObject jsonObject = new JSONObject((HashMap<?, ?>) item);
                        return jsonObject.optString("description", "No disponible");
                    }
                    return "No disponible";
                })
                .collect(Collectors.joining(", "));
    }

    private String extractPlatforms(JSONObject platforms) {
        StringBuilder platformStr = new StringBuilder();
        if (platforms != null) {
            if (platforms.optBoolean("windows")) platformStr.append("Windows ");
            if (platforms.optBoolean("mac")) platformStr.append("Mac ");
            if (platforms.optBoolean("linux")) platformStr.append("Linux");
        }
        return platformStr.length() > 0 ? platformStr.toString() : "No disponible";
    }

    private String normalizeGameName(String gameName) {
        return gameName.trim().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    private String findBestMatch(String userInput, JSONArray steamGameList) {
        LevenshteinDistance distance = new LevenshteinDistance();
        String bestMatch = null;
        int lowestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < steamGameList.length(); i++) {
            JSONObject game = steamGameList.getJSONObject(i);
            String gameName = game.getString("name");
            int currentDistance = distance.apply(userInput, gameName);

            if (currentDistance < lowestDistance) {
                lowestDistance = currentDistance;
                bestMatch = gameName;
            }
        }

        return bestMatch;
    }

    private JSONObject searchGameOnSteam(String gameName) throws Exception {
        try {
            String encodedGameName = URLEncoder.encode(gameName, "UTF-8");
            String apiUrl = STEAM_API_URL_SEARCHGAME_PART1_INIT + encodedGameName + STEAM_API_URL_SEARCHGAME_PART2_LANGUAGE_AND_REGION ;
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(ErrorMessages.HTTP_ERROR_MESSAGE + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONObject json = new JSONObject(content.toString());
            JSONArray games = json.getJSONArray("items");
            if (games.length() > 0) {
                String bestMatch = findBestMatch(gameName, games);
                for (int i = 0; i < games.length(); i++) {
                    JSONObject game = games.getJSONObject(i);
                    if (game.getString("name").equalsIgnoreCase(bestMatch)) {
                        int appId = game.getInt("id");
                        return getGameDetails(appId);
                    }
                }
                return null;
            } else {
                throw new GameNotFoundException();
            }
        } catch (IOException | JSONException e) {
            System.err.println(ErrorMessages.DONT_FOUND_GAME_DETAILS_ERROR_MESSAGE + e.getMessage());
            throw new Exception(ErrorMessages.GAME_DETAILS_RETRY_ERROR_MESSAGE);
        }
    }

    private JSONObject getDLCInfo(int dlcId) throws Exception {
        try {
            String apiUrl = STEAM_API_URL_PART1_INIT + dlcId + STEAM_API_URL_SEARCHGAME_REGION_AR;
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(ErrorMessages.HTTP_ERROR_MESSAGE + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONObject json = new JSONObject(content.toString());
            return json.getJSONObject(String.valueOf(dlcId)).getJSONObject("data");
        } catch (IOException | JSONException e) {
            System.err.println(ErrorMessages.DLC_DETAILS_ERROR_MESSAGE + e.getMessage());
            throw new Exception(ErrorMessages.DLC_DETAILS_RETRI_ERROR_MESSAGE);
        }
    }

    static class GameNotFoundException extends Exception {
        public GameNotFoundException() {
            super("No se encontró el juego.");
        }
    }
}
