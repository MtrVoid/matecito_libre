package org.example.commands.monetarycommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;

public class CurrencyConverterCommand extends ListenerAdapter {

    private JSONObject exchangeRateCache;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("convertir"))
        {
            OptionMapping fromCurrencyOption = event.getOption("de");
            OptionMapping toCurrencyOption = event.getOption("a");
            OptionMapping amountOption = event.getOption("cantidad");

            if (fromCurrencyOption == null || toCurrencyOption == null) {
                event.reply("Debes proporcionar ambas monedas para la conversión.").queue();
                return;
            }

            String fromCurrency = fromCurrencyOption.getAsString().toUpperCase();
            String toCurrency = toCurrencyOption.getAsString().toUpperCase();

            int amount = amountOption != null ? amountOption.getAsInt() : 1;

            event.deferReply().queue();

            CompletableFuture.supplyAsync(() -> {
                try {
                    return getExchangeRate(fromCurrency, toCurrency);
                } catch (Exception e) {
                    return null;
                }
            }).thenAccept(exchangeRate -> {
                if (exchangeRate != null) {
                    handleCurrencyConversion(event, exchangeRate, fromCurrency, toCurrency, amount);
                } else {
                    sendCurrencyInformation(event);
                }
            }).thenRun(this::clearCache);
        }
    }

    private void handleCurrencyConversion(SlashCommandInteractionEvent event, double exchangeRate, String fromCurrency, String toCurrency, int amount) {
        double convertedAmount = amount * exchangeRate;

        DecimalFormat df = new DecimalFormat("#.##");
        String formattedRate = df.format(exchangeRate);
        String formattedAmount = df.format(convertedAmount);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Conversión de Moneda");
        embed.setDescription(amount + " " + fromCurrency + " equivalen a " + formattedAmount + " " + toCurrency
                + "\n(1 " + fromCurrency + " = " + formattedRate + " " + toCurrency + ")");
        embed.setColor(Color.CYAN);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendCurrencyInformation(SlashCommandInteractionEvent event) {
        String infoUrl = "https://www.xe.com/symbols.php";

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Moneda no reconocida o error en la conversión");
        embed.setDescription("Puedes encontrar una lista de monedas válidas en: " + infoUrl);
        embed.setColor(Color.RED);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    public double getExchangeRate(String fromCurrency, String toCurrency) throws Exception {
        if (exchangeRateCache != null && exchangeRateCache.has(fromCurrency)) {
            JSONObject rates = exchangeRateCache.getJSONObject(fromCurrency).getJSONObject("rates");
            return rates.getDouble(toCurrency);
        }

        String apiUrl = "https://api.exchangerate-api.com/v4/latest/" + fromCurrency;
        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        JSONObject json = new JSONObject(content.toString());
        exchangeRateCache = json;

        JSONObject rates = json.getJSONObject("rates");
        return rates.getDouble(toCurrency);
    }

    private void clearCache() {
        exchangeRateCache = null;
    }
}
