package com.example.asset.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssetService {
    private static final String ASSET_DATA_FILE = "/assets.csv";

    public List<Map<String, String>> findAll() {
        InputStream data = AssetService.class.getResourceAsStream(ASSET_DATA_FILE);
        if (data == null) {
            throw new IllegalStateException("Asset data file not found: " + ASSET_DATA_FILE);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(data, StandardCharsets.UTF_8))) {
            return readAssets(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read asset data", exception);
        }
    }

    private List<Map<String, String>> readAssets(BufferedReader reader) throws IOException {
        List<Map<String, String>> assets = new ArrayList<>();
        String line;
        boolean header = true;

        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }

            String[] columns = line.split(",", -1);
            if (columns.length != 3) {
                throw new IllegalStateException("Invalid asset row: " + line);
            }

            Map<String, String> asset = new LinkedHashMap<>();
            asset.put("symbol", columns[0].trim());
            asset.put("name", columns[1].trim());
            asset.put("quantity", columns[2].trim());
            assets.add(asset);
        }

        return assets;
    }
}
