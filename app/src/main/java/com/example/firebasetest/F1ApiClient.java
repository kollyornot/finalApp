package com.example.firebasetest;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class F1ApiClient {
    private static final String BASE_URL = "https://api.jolpi.ca/ergast/f1";
    private Context context;

    public F1ApiClient(Context context) {
        this.context = context;
    }

    public interface RaceCallback {
        void onSuccess(RaceData race);
        void onError(String error);
    }

    public void getNextRace(RaceCallback callback) {
        String url = BASE_URL + "/current/next.json";

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject mrData = response.getJSONObject("MRData");
                        JSONObject raceTable = mrData.getJSONObject("RaceTable");
                        JSONArray races = raceTable.getJSONArray("Races");

                        if (races.length() > 0) {
                            JSONObject race = races.getJSONObject(0);
                            JSONObject circuit = race.getJSONObject("Circuit");

                            RaceData raceData = new RaceData();
                            raceData.setRaceName(race.getString("raceName"));
                            raceData.setCircuitName(circuit.getString("circuitName"));
                            raceData.setDate(race.getString("date"));
                            raceData.setTime(race.optString("time", "00:00:00Z"));
                            raceData.setTimestampFromDateTime();

                            callback.onSuccess(raceData);
                        } else {
                            callback.onError("no data");
                        }
                    } catch (JSONException e) {
                        callback.onError("parsing error: " + e.getMessage());
                    }
                },
                error -> callback.onError("network error: " + error.getMessage())
        );

        queue.add(request);
    }

}

