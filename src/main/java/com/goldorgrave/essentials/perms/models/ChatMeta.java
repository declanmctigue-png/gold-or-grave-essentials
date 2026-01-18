// File: com/goldorgrave/essentials/perms/models/ChatMeta.java
package com.goldorgrave.essentials.perms.models;

public final class ChatMeta {
    private final String prefix;
    private final String prefixColor;
    private final String prefixFormat;

    private final String suffix;
    private final String suffixColor;
    private final String suffixFormat;

    private final String playerColor;
    private final String playerFormat;

    private final String chatColor;
    private final String chatFormat;

    public ChatMeta(
            String prefix, String prefixColor, String prefixFormat,
            String suffix, String suffixColor, String suffixFormat,
            String playerColor, String playerFormat,
            String chatColor, String chatFormat
    ) {
        this.prefix = prefix != null ? prefix : "";
        this.prefixColor = prefixColor != null ? prefixColor : "";
        this.prefixFormat = prefixFormat != null ? prefixFormat : "";

        this.suffix = suffix != null ? suffix : "";
        this.suffixColor = suffixColor != null ? suffixColor : "";
        this.suffixFormat = suffixFormat != null ? suffixFormat : "";

        this.playerColor = playerColor != null ? playerColor : "";
        this.playerFormat = playerFormat != null ? playerFormat : "";

        this.chatColor = chatColor != null ? chatColor : "";
        this.chatFormat = chatFormat != null ? chatFormat : "";
    }

    public static ChatMeta empty() {
        return new ChatMeta("", "", "", "", "", "", "", "", "", "");
    }

    public String getPrefix() { return prefix; }
    public String getPrefixColor() { return prefixColor; }
    public String getPrefixFormat() { return prefixFormat; }

    public String getSuffix() { return suffix; }
    public String getSuffixColor() { return suffixColor; }
    public String getSuffixFormat() { return suffixFormat; }

    public String getPlayerColor() { return playerColor; }
    public String getPlayerFormat() { return playerFormat; }

    public String getChatColor() { return chatColor; }
    public String getChatFormat() { return chatFormat; }
}
