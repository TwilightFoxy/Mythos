package com.twily.mythos.gameplay;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class MythStatusHelper {

    private MythStatusHelper() {
    }

    public static void syncModifier(LivingEntity entity, net.minecraft.core.Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation, boolean active) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        if (active) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }
}
