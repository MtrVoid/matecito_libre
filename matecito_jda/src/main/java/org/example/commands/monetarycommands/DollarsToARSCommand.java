package org.example.commands.monetarycommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.utilities.CurrencyConverter;

public class DollarsToARSCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("dollarsto_ars")) {
            String tipoDolar = event.getOption("tipo") != null ? event.getOption("tipo").getAsString() : "oficial";
            String montoStr = event.getOption("monto") != null ? event.getOption("monto").getAsString() : "1.0";

            montoStr = montoStr.replace(",", ".");
            double montoUSD;
            try {
                montoUSD = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                event.reply("Por favor, introduce un número válido para el monto.").queue();
                return;
            }

            double resultado = CurrencyConverter.convertToARS(montoUSD, tipoDolar);

            if (resultado == -1) {
                event.reply("Hubo un error al obtener la tasa de conversión. Inténtalo nuevamente.").queue();
            } else {
                event.reply(String.format("La conversión de %.2f USD a ARS usando el dólar %s es: %.2f ARS", montoUSD, tipoDolar, resultado)).queue();
            }
        }
    }
}
