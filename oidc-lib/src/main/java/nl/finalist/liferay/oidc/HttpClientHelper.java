package nl.finalist.liferay.oidc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpClientHelper {

    private HttpClientHelper() {
    }

    static String getResponseStringFromConnection(HttpURLConnection conn) throws IOException {
        BufferedReader bufferedReader;
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder stringBuilder= new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    static JSONObject processResponse(int responseCode, String response) throws JSONException {

        JSONObject responseJson = new JSONObject();
        responseJson.put("responseCode", responseCode);

        if (response.equalsIgnoreCase("")) {
            responseJson.put("responseMsg", "");
        } else {
            responseJson.put("responseMsg", new JSONObject(response));
        }
        return responseJson;
    }
}