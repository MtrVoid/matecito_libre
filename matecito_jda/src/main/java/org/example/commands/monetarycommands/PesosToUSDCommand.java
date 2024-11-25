package org.example.commands.monetarycommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.utilities.CurrencyConverter;

public class PesosToUSDCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("pesosto_usd")) {
            String tipoDolar = event.getOption("tipo") != null ? event.getOption("tipo").getAsString() : "oficial";
            String montoStr = event.getOption("monto") != null ? event.getOption("monto").getAsString() : "1.0";

            // Reemplaza comas por puntos para manejar entradas con coma
            montoStr = montoStr.replace(",", ".");
            double montoARS;
            try {
                montoARS = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                event.reply("Por favor, introduce un número válido para el monto.").queue();
                return;
            }

            double conversionRate = 0;
            switch (tipoDolar.toLowerCase()) {
                case "tarjeta":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("tarjeta");
                    break;
                case "cripto":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("cripto");
                    break;
                case "blue":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("blue");
                    break;
                case "bolsa":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("bolsa");
                    break;
                case "cll":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("cll");
                    break;
                case "mayorista":
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("mayorista");
                    break;
                case "oficial":
                default:
                    conversionRate = CurrencyConverter.getARSToUSDConversionRate("oficial");
                    break;
            }

            if (conversionRate <= 0) {
                event.reply("Hubo un error al obtener la tasa de conversión. Inténtalo nuevamente.").queue();
            } else {
                double resultado = montoARS / conversionRate;
                event.reply(String.format("La conversión de %.2f ARS a USD usando el dólar %s es: %.2f USD", montoARS, tipoDolar, resultado)).queue();
            }
        }
    }
}


