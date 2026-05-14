package com.twily.mythos.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class MythosSkillKeys {

    private static final KeyMapping SKILL_1 = new KeyMapping(
        "key.mythos.skill_1",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        MythosKeyCategory.MYTHOS
    );
    private static final KeyMapping SKILL_2 = new KeyMapping(
        "key.mythos.skill_2",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        MythosKeyCategory.MYTHOS
    );
    private static final KeyMapping SKILL_3 = new KeyMapping(
        "key.mythos.skill_3",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        MythosKeyCategory.MYTHOS
    );

    private MythosSkillKeys() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SKILL_1);
        event.register(SKILL_2);
        event.register(SKILL_3);
    }

    public static KeyMapping skill1() {
        return SKILL_1;
    }

    public static KeyMapping skill2() {
        return SKILL_2;
    }

    public static KeyMapping skill3() {
        return SKILL_3;
    }

    public static Component skill1Name() {
        return SKILL_1.getTranslatedKeyMessage();
    }

    public static Component skill2Name() {
        return SKILL_2.getTranslatedKeyMessage();
    }

    public static Component skill3Name() {
        return SKILL_3.getTranslatedKeyMessage();
    }
}
