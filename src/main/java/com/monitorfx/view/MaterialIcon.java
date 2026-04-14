/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.view;

/**
 *
 * @author HP
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/**
 * Charge des icônes Material locales au format SVG.
 * Les fichiers SVG sont stockés dans les resources.
 */
public class MaterialIcon extends StackPane {

    public enum Type {
        REFRESH("refresh.svg"),
        SYNC_ALT("sync_alt.svg"),
        BLOCK("block.svg"),
        MEMORY("memory.svg"),
        MONITORING("monitoring.svg"),
        INFO("info.svg"),
        SEARCH("search.svg"),
        LIST_ALT("list_alt.svg"),
        DESKTOP_WINDOWS("desktop_windows.svg"),
        SETTINGS("settings.svg"),
        ACCOUNT_CIRCLE("account_circle.svg");

        private final String resourceName;

        Type(String resourceName) {
            this.resourceName = resourceName;
        }

        public String resourcePath() {
            return "/icons/" + resourceName;
        }
    }

    private static final Pattern PATH_PATTERN = Pattern.compile("<path[^>]*d=\"([^\"]+)\"");
    private static final Map<Type, String> PATH_CACHE = new EnumMap<>(Type.class);

    private final SVGPath shape = new SVGPath();

    public MaterialIcon(Type type, double size) {
        getStyleClass().add("material-icon");
        shape.getStyleClass().add("material-icon-shape");
        getChildren().add(shape);
        setIcon(type, size);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    public void setIcon(Type type, double size) {
        shape.setContent(loadPath(type));
        double scale = size / 24.0;
        shape.setScaleX(scale);
        shape.setScaleY(scale);
    }

    private String loadPath(Type type) {
        return PATH_CACHE.computeIfAbsent(type, key -> {
            try (InputStream inputStream = MaterialIcon.class.getResourceAsStream(key.resourcePath())) {
                if (inputStream == null) {
                    throw new IllegalStateException("Icône introuvable : " + key.resourcePath());
                }
                String svg = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Matcher matcher = PATH_PATTERN.matcher(svg);
                if (!matcher.find()) {
                    throw new IllegalStateException("Aucun path SVG trouvé dans " + key.resourcePath());
                }
                return matcher.group(1);
            } catch (IOException exception) {
                throw new IllegalStateException("Impossible de charger l'icône : " + key.resourcePath(), exception);
            }
        });
    }
}

