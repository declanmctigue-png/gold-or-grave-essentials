package com.goldorgrave.essentials.util;

import com.hypixel.hytale.server.core.Message;




public final class ColorText {
    private ColorText() {}
    public static Message parse(String input) {
        return MessageUtil.legacy(input);
    }
}