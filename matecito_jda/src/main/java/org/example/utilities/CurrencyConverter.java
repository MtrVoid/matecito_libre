package org.example.utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.util.Map;

public class CurrencyConverter {

    private static final Map<String, String> API_URLS = Map.of(
            "tarjeta", "https://dolarapi.com/v1/dolares/tarjeta",
            "cripto", "https://dolarapi.com/v1/dolares/cripto",
            "blue", "https://dolarapi.com/v1/dolares/blue",
            "bolsa", "https://dolarapi.com/v1/dolares/bolsa",
            "cll", "https://dolarapi.com/v1/dolares/contadoconliqui",
            "mayorista", "https://dolarapi.com/v1/dolares/mayorista",
            "oficial", "https://dolarapi.com/v1/dolares/oficial"
    );

    public static double convertToARS(double amountInUSD, String tipoDolar) {
        String apiUrl = API_URLS.getOrDefault(tipoDolar.toLowerCase(), API_URLS.get("oficial"));
        double conversionRate = fetchConversionRate(apiUrl);
        return conversionRate > 0 ? amountInUSD * conversionRate : -1;
    }

    public static double getARSToUSDConversionRate(String tipoDolar) {
        String apiUrl = API_URLS.getOrDefault(tipoDolar.toLowerCase(), API_URLS.get("oficial"));
        return fetchConversionRate(apiUrl);
    }

    private static double fetchConversionRate(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error en la solicitud HTTP: " + responseCode);
                return -1;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            con.disconnect();

            JSONObject json = new JSONObject(content.toString());
            if (!json.has("venta")) {
                System.err.println("Campo 'venta' no encontrado en la respuesta JSON");
                return -1;
            }

            return json.getDouble("venta");

        } catch (Exception e) {
            System.err.println("Error durante la obtención de la tasa de conversión: " + e.getMessage());
            return -1;
        }
    }
}
