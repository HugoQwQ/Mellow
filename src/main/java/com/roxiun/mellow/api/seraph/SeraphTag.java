package com.roxiun.mellow.api.seraph;

public class SeraphTag {

    private final String icon;
    private final String tooltip;
    private final int color;
    private final String tagName;
    private final String text;
    private final int textColor;

    public SeraphTag(String icon, String tooltip, int color, String tagName, String text, int textColor) {
        this.icon = icon;
        this.tooltip = tooltip;
        this.color = color;
        this.tagName = tagName;
        this.text = text;
        this.textColor = textColor;
    }

    public String getIcon() {
        return icon;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getColor() {
        return color;
    }

    public String getTagName() {
        return tagName;
    }

    public String getText() {
        return text;
    }

    public int getTextColor() {
        return textColor;
    }
}
