package org.example.commands.monetarycommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.awt.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class DolarTodayCommand extends ListenerAdapter {

    private static final HttpClient client = HttpClient.newHttpClient();
    private String oficial = "";
    private String blue = "";
    private String bolsa = "";
    private String contadoConLiqui = "";
    private String tarjeta = "";
    private String mayorista = "";
    private String cripto = "";

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("dolarhoy")) {
            CompletableFuture.allOf(
                    fetchDollarData("https://dolarapi.com/v1/dolares/oficial", "Oficial"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/blue", "Blue"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/bolsa", "Bolsa"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/contadoconliqui", "Contado con Liqui"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/tarjeta", "Tarjeta"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/mayorista", "Mayorista"),
                    fetchDollarData("https://dolarapi.com/v1/dolares/cripto", "Cripto")
            ).thenRun(() -> {
                EmbedBuilder embed = buildResponseEmbed();
                event.replyEmbeds(embed.build()).queue();

                clearCache();
            });
        }
    }

    private CompletableFuture<Void> fetchDollarData(String url, String type) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    JSONObject json = new JSONObject(response);
                    double compra = json.getDouble("compra");
                    double venta = json.getDouble("venta");
                    String fechaActualizacion = json.getString("fechaActualizacion");
                    String formattedData = String.format("Compra: %.2f\nVenta: %.2f\nFecha: %s", compra, venta, fechaActualizacion);

                    switch (type) {
                        case "Oficial" -> oficial = formattedData;
                        case "Blue" -> blue = formattedData;
                        case "Bolsa" -> bolsa = formattedData;
                        case "Contado con Liqui" -> contadoConLiqui = formattedData;
                        case "Tarjeta" -> tarjeta = formattedData;
                        case "Mayorista" -> mayorista = formattedData;
                        case "Cripto" -> cripto = formattedData;
                    }
                });
    }

    private EmbedBuilder buildResponseEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📣 Cotizaciones del Dólar Hoy");
        embed.setColor(Color.GREEN);

        embed.addField("💵 Dólar Oficial", oficial.isEmpty() ? "No disponible" : oficial, false);
        embed.addField("💵 Dólar Blue", blue.isEmpty() ? "No disponible" : blue, false);
        embed.addField("💵 Dólar Bolsa", bolsa.isEmpty() ? "No disponible" : bolsa, false);
        embed.addField("💵 Contado con Liqui", contadoConLiqui.isEmpty() ? "No disponible" : contadoConLiqui, false);
        embed.addField("💵 Dólar Tarjeta", tarjeta.isEmpty() ? "No disponible" : tarjeta, false);
        embed.addField("💵 Dólar Mayorista", mayorista.isEmpty() ? "No disponible" : mayorista, false);
        embed.addField("💵 Dólar Cripto", cripto.isEmpty() ? "No disponible" : cripto, false);

        return embed;
    }

    // Método para limpiar el caché después de la ejecución
    private void clearCache() {
        oficial = "";
        blue = "";
        bolsa = "";
        contadoConLiqui = "";
        tarjeta = "";
        mayorista = "";
        cripto = "";
    }
}
