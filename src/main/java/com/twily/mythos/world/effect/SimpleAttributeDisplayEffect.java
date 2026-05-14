package com.twily.mythos.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class SimpleAttributeDisplayEffect extends MobEffect {

    public SimpleAttributeDisplayEffect(
        MobEffectCategory category,
        int color,
        Holder<Attribute> attribute,
        Identifier modifierId,
        double amount,
        AttributeModifier.Operation operation
    ) {
        super(category, color);
        this.addAttributeModifier(attribute, modifierId, amount, operation);
    }
}
