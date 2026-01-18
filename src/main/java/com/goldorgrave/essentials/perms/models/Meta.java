package com.goldorgrave.essentials.perms.models;

public final class Meta {

    private String prefix = "";
    private String suffix = "";

    // Hex colors as strings. Examples: "#ff5555" or "ff5555"
    // Keep them blank to mean "no explicit color"
    private String prefixColor = "";
    private String suffixColor = "";

    private String playerColor = "";
    private String chatColor = "";

    public Meta() {}

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix != null ? prefix : ""; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix != null ? suffix : ""; }

    public String getPrefixColor() { return prefixColor; }
    public void setPrefixColor(String prefixColor) { this.prefixColor = prefixColor != null ? prefixColor : ""; }

    public String getSuffixColor() { return suffixColor; }
    public void setSuffixColor(String suffixColor) { this.suffixColor = suffixColor != null ? suffixColor : ""; }

    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor != null ? playerColor : ""; }

    public String getChatColor() { return chatColor; }
    public void setChatColor(String chatColor) { this.chatColor = chatColor != null ? chatColor : ""; }
}
