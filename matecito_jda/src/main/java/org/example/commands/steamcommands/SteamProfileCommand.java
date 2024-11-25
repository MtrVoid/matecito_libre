package org.example.commands.steamcommands;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SteamProfileCommand extends ListenerAdapter {
    private Dotenv config;

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String STEAM_GAME_NAME_SEARCH = "https://store.steampowered.com/api/appdetails?appids=";

    public Dotenv getConfig() {
        return config;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        config = Dotenv.configure().load();
        String steamKey = config.get("STEAMKEY");

        if (event.getName().equals("steamusuario")) {
            String steamNickname = event.getOption("steamid").getAsString();
            event.deferReply().queue();

            String steamID = resolveVanityURL(steamNickname, steamKey);

            if (steamID == null) {
                if (steamNickname.matches("\\d+")) {
                    steamID = steamNickname;
                } else {
                    event.getHook().sendMessage("No se pudo resolver el Steam ID para el nickname proporcionado.").queue();
                    return;
                }
            }

            String profileData = getPlayerSummaries(steamID, steamKey);
            String recentlyPlayedGamesData = getRecentlyPlayedGames(steamID, steamKey);

            if (profileData != null) {
                JSONObject player = extractPlayerData(profileData);

                if (player != null) {
                    EmbedBuilder embed = buildProfileEmbed(player, steamID);

                    addRecentlyPlayedGames(embed, recentlyPlayedGamesData);

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    embed.clear();
                } else {
                    event.getHook().sendMessage("🚫 No se encontró el perfil en Steam o el usuario no existe.").queue();
                }

                player.clear();
            } else {
                event.getHook().sendMessage("🚫 No se encontró el perfil en Steam.").queue();
            }

            recentlyPlayedGamesData = null;
            profileData = null;
        }
    }

    private String resolveVanityURL(String nickname, String steamKey) {
        String url = "http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=" + steamKey + "&vanityurl=" + nickname;
        return sendSteamRequest(url, json -> json.getJSONObject("response").getInt("success") == 1
                ? json.getJSONObject("response").getString("steamid") : null);
    }

    private String getPlayerSummaries(String steamID, String steamKey) {
        String url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamKey + "&steamids=" + steamID;
        return sendSteamRequest(url, json -> json.toString());
    }

    private String getRecentlyPlayedGames(String steamID, String steamKey) {
        String url = "http://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=" + steamKey + "&steamid=" + steamID + "&format=json";
        return sendSteamRequest(url, json -> json.toString());
    }

    private String sendSteamRequest(String url, SteamResponseHandler handler) {
        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                return handler.handleResponse(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject extractPlayerData(String profileData) {
        JSONObject json = new JSONObject(profileData);
        JSONArray players = json.getJSONObject("response").optJSONArray("players");
        return players != null && players.length() > 0 ? players.getJSONObject(0) : null;
    }

    private EmbedBuilder buildProfileEmbed(JSONObject player, String steamID) {
        EmbedBuilder embed = new EmbedBuilder();
        String personaName = player.getString("personaname");
        String profileUrl = player.getString("profileurl");
        String avatar = player.getString("avatarfull");

        embed.setTitle("🔗 Perfil de " + personaName, profileUrl);
        embed.setThumbnail("https://media.discordapp.net/attachments/815395979585912842/1285999280045297786/image-removebg-preview_9.png?format=webp");
        embed.setImage(avatar);
        embed.addField("👤 Nombre de usuario", personaName, false);
        embed.addField("💾 SteamID", steamID, false);
        embed.addField("💯 Perfil de Steam", "[Ver perfil](" + profileUrl + ")", false);

        long lastLogoff = player.optLong("lastlogoff", -1);
        if (lastLogoff != -1) {
            embed.addField("⏳ Última desconexión", "<t:" + lastLogoff + ":R>", false);
        }

        int personaState = player.optInt("personastate", -1);
        embed.addField("🟢 Estado", getPersonaStateString(personaState), false);

        return embed;
    }

    private void addRecentlyPlayedGames(EmbedBuilder embed, String recentlyPlayedGamesData) {
        if (recentlyPlayedGamesData != null) {
            JSONObject gamesJson = new JSONObject(recentlyPlayedGamesData);
            if (gamesJson.getJSONObject("response").has("games")) {
                JSONArray games = gamesJson.getJSONObject("response").getJSONArray("games");

                StringBuilder gamesList = new StringBuilder();
                for (int i = 0; i < Math.min(games.length(), 10); i++) {
                    JSONObject game = games.getJSONObject(i);
                    String gameName = getGameName(game.getInt("appid"));
                    int playtime = game.getInt("playtime_forever") / 60;
                    gamesList.append(String.format("%s - %d horas\n", gameName, playtime));
                }
                embed.addField("🔥 Últimos juegos jugados", gamesList.toString(), false);
            } else {
                embed.addField("🔥 Últimos juegos jugados", "🚫 No hay juegos recientes", false);
            }
        } else {
            embed.addField("🔥 Últimos juegos jugados", "🚫 No se pudo obtener la información de juegos recientes", false);
        }
    }

    private String getGameName(int appid) {
        String url = STEAM_GAME_NAME_SEARCH + appid;
        return sendSteamRequest(url, json -> json.getJSONObject(String.valueOf(appid)).getJSONObject("data").getString("name"));
    }

    private String getPersonaStateString(int personaState) {
        switch (personaState) {
            case 0: return "Desconectado";
            case 1: return "En línea";
            case 2: return "Ocupado";
            case 3: return "Ausente";
            case 4: return "Durmiendo";
            case 5: return "Buscando partida";
            case 6: return "Jugando";
            default: return "Desconocido";
        }
    }

    @FunctionalInterface
    private interface SteamResponseHandler {
        String handleResponse(JSONObject json);
    }
}
