package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.example.commands.*;
import org.example.commands.monetarycommands.CurrencyConverterCommand;
import org.example.commands.monetarycommands.DolarTodayCommand;
import org.example.commands.monetarycommands.DollarsToARSCommand;
import org.example.commands.monetarycommands.PesosToUSDCommand;
import org.example.commands.steamcommands.steamworld.SteamAmericaCommand;

import org.example.commands.steamcommands.SteamcitoCommand;
import org.example.commands.steamcommands.SteamProfileCommand;
import org.example.commands.steamcommands.SteamUSACommand;

import javax.security.auth.login.LoginException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainBot {
    private final Dotenv config;
    private final ShardManager shardManager;
    private final ScheduledExecutorService scheduler;


    public MainBot() throws LoginException {
        config = Dotenv.configure().load();
        String token = config.get("TOKEN");
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("Steam Argentina"));
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);

        shardManager = builder.build();

        shardManager.addEventListener(
                // Monetary Commands
                new CurrencyConverterCommand(),
                new DollarsToARSCommand(),
                new DolarTodayCommand(),
                new PesosToUSDCommand(),

                // Steam World
                new SteamAmericaCommand(),

                // Steam Commands
                new SteamcitoCommand(),
                new SteamProfileCommand(),
                new SteamUSACommand(),

                // Commands
                new CreditsCommand(),
                new HeavyCalculatorCommand(),
                new InfoCommand()
        );

        shardManager.getShards().forEach(jda -> {
            jda.updateCommands()
                    .addCommands(
                            // Monetary Commands
                            Commands.slash("convertir", "Convierte una moneda a otra")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "de", "Moneda de origen", true),
                                            new OptionData(OptionType.STRING, "a", "Moneda de destino", true),
                                            new OptionData(OptionType.INTEGER, "cantidad", "Cantidad a convertir (opcional, por defecto es 1)", false)
                                    ),
                            jda.upsertCommand("dollarsto_ars", "Convierte USD a ARS según el tipo de dólar seleccionado")
                                    .addOptions(new OptionData(OptionType.STRING, "tipo", "Tipo de dólar: tarjeta, cripto, oficial", true)
                                            .addChoice("Dólar Tarjeta", "tarjeta")
                                            .addChoice("Dólar Cripto", "cripto")
                                            .addChoice("Dolar Blue", "blue")
                                            .addChoice("Dolar Bolsa", "Bolsa")
                                            .addChoice("Dolar CLL", "cll")
                                            .addChoice("Dolar Mayorista", "mayorista")
                                            .addChoice("Dolar Oficial", "oficial"))
                                    .addOption(OptionType.STRING, "monto", "Monto en USD", true),
                            Commands.slash("dolarhoy", "Muestra los tipos de cambio del dólar (compra y venta)"),
                            jda.upsertCommand("pesosto_usd", "Convierte ARS a USD según el tipo de dólar seleccionado")
                                    .addOptions(new OptionData(OptionType.STRING, "tipo", "Tipo de dólar: tarjeta, cripto, oficial", true)
                                            .addChoice("Dólar Tarjeta", "tarjeta")
                                            .addChoice("Dólar Cripto", "cripto")
                                            .addChoice("Dolar Blue", "blue")
                                            .addChoice("Dolar Bolsa", "Bolsa")
                                            .addChoice("Dolar CLL", "cll")
                                            .addChoice("Dolar Mayorista", "mayorista")
                                            .addChoice("Dolar Oficial", "oficial"))
                                    .addOption(OptionType.STRING, "monto", "Monto en USD", true),

                            // Steam Commands
                            Commands.slash("steamcito", "Busca un juego en Steam y muestra su precio en ARS")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "mostrar", "Elije cuanta cantidad de información quieres ver", true)
                                                    .addChoice("📨 Concreto", "mostrar_concreto")
                                                    .addChoice("🔎 Extenso", "mostrar_extenso"),
                                            new OptionData(OptionType.STRING, "conversion", "Tipo de conversión. (Al elegir 'Cripto' no es necesario el Impuesto Provincial)", true)
                                                    .addChoice("💳 Dólar Tarjeta", "tarjeta")
                                                    .addChoice("🪙 Dólar Cripto", "cripto")
                                                    .addChoice("🏦 Dólar Oficial (No Utilizable)", "oficial"),
                                            new OptionData(OptionType.STRING, "juego", "El nombre del juego a buscar", true),
                                            new OptionData(OptionType.STRING, "impuestoprovincial", "Porcentaje de impuestos provinciales (Ponlo sin el % y que no sea decimal)", false)
                                    ),
                            Commands.slash("steamusuario", "Obtiene el perfil de Steam")
                                    .addOption(OptionType.STRING, "steamid", "El SteamID del usuario en Steam", true),
                            Commands.slash("steam_america", "Busca un juego en Steam y muestra su precio en ARS")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "juego", "El nombre del juego a buscar", true),
                                            new OptionData(OptionType.STRING, "region", "Región donde quieres buscar el juego.", true)
                                                    .addChoice("América - Brasil", "BR")
                                                    .addChoice("América - Canadá", "CA")
                                                    .addChoice("América - Chile", "CL")
                                                    .addChoice("América - Colombia", "CO")
                                                    .addChoice("América - Costa Rica", "CR")
                                                    .addChoice("América - Ecuador", "EC")
                                                    .addChoice("América - Guatemala", "GT")
                                                    .addChoice("América - Honduras", "HN")
                                                    .addChoice("América - México", "MX")
                                                    .addChoice("América - Nicaragua", "NI")
                                                    .addChoice("América - Panamá", "PA")
                                                    .addChoice("América - Perú", "PE")
                                                    .addChoice("América - Paraguay", "PY")
                                                    .addChoice("América - El Salvador", "SV")
                                                    .addChoice("América - Estados Unidos", "US")
                                                    .addChoice("América - Uruguay", "UY")
                                                    .addChoice("América - Venezuela", "VE")
                                    ),
                            Commands.slash("steam_africa", "Busca un juego en Steam y muestra su precio en ARS")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "juego", "El nombre del juego a buscar", true),
                                            new OptionData(OptionType.STRING, "region", "Región donde quieres buscar el juego.", true)
                                                    // África
                                                    .addChoice("África - Sudáfrica", "ZA")
                                    ),
                            Commands.slash("steamusa", "Busca información sobre un juego en Steam")
                                    .addOption(OptionType.STRING, "juego", "El nombre del juego a buscar", true),

                            // Otros Commands
                            Commands.slash("creditos", "📜 Aquí verás los créditos de Matecito."),
                            Commands.slash("calculadora", "Realiza cálculos matemáticos")
                                    .addOption(OptionType.STRING, "expresiones", "La expresión matemática a evaluar", true),
                            Commands.slash("info", "Información sobre los comandos de este bot.")
                    )
                    .queue(
                            success -> System.out.println("Comandos actualizados exitosamente."),
                            error -> System.err.println("Error al actualizar comandos: " + error.getMessage())
                    );
        });

        scheduler = Executors.newScheduledThreadPool(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Apagando bot de forma limpia...");
                scheduler.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public Dotenv getConfig() {
        return config;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public void shutdown() {
        shardManager.shutdown();
        scheduler.shutdown();
    }

    public static void main(String[] args) {
        try {
            MainBot bot = new MainBot();
            Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown));
        } catch (LoginException e) {
            System.err.println("ERROR: Provided bot TOKEN is invalid!");
            e.printStackTrace();
        }
    }
}
