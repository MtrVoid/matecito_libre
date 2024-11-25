package org.example.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.awt.*;

public class HeavyCalculatorCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("calculadora")) {
            String expressionString = event.getOption("expresiones").getAsString();

            try {
                Expression expression = new ExpressionBuilder(expressionString).build();

                double result = expression.evaluate();

                event.reply("El resultado es: " + result).queue();
            } catch (Exception e) {
                sendOperatorInformation(event);
            }
        }
    }

    private void sendOperatorInformation(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error en la expresión");
        embed.setDescription("Verifica la expresión e inténtalo de nuevo. Aquí tienes algunos operadores y funciones disponibles:");
        embed.addField("Operadores aritméticos básicos",
                "Suma: `+`\n" +
                        "Resta: `-`\n" +
                        "Multiplicación: `*`\n" +
                        "División: `/`\n" +
                        "Módulo (resto de la división): `%`", false);
        embed.addField("Operadores lógicos",
                "AND (y lógico): `&`\n" +
                        "OR (o lógico): `|`\n" +
                        "NOT (negación): `!`", false);
        embed.addField("Operadores de comparación",
                "Menor que: `<`\n" +
                        "Menor o igual que: `<=`\n" +
                        "Mayor que: `>`\n" +
                        "Mayor o igual que: `>=`\n" +
                        "Igual a: `==`\n" +
                        "Distinto de: `!=`", false);
        embed.addField("Otros operadores",
                "Potencia: `^`\n" +
                        "Paréntesis: `( )` para agrupar operaciones y controlar la precedencia\n" +
                        "Factorial: `!` (por ejemplo, `5!`)", false);
        embed.addField("Funciones matemáticas",
                "`abs(x)`: Valor absoluto de x\n" +
                        "`sqrt(x)`: Raíz cuadrada de x\n" +
                        "`sin(x)`, `cos(x)`, `tan(x)`: Funciones trigonométricas\n" +
                        "`asin(x)`, `acos(x)`, `atan(x)`: Funciones trigonométricas inversas\n" +
                        "`log(x)`: Logaritmo natural de x\n" +
                        "`exp(x)`: Función exponencial (e^x)\n" +
                        "Y muchas más...", false);
        embed.setColor(Color.CYAN);

        event.replyEmbeds(embed.build()).queue();
    }
}
