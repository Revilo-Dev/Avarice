package com.revilo.gatesofavarice.integration;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class AuraProgressionIntegration {
    private AuraProgressionIntegration() {}

    public static void resetSkillsAndAbilities(ServerPlayer player) {
        if (!ModCompat.isAnyLoaded("aura")) {
            return;
        }
        try {
            resetAttachment(player, "net.revilodev.aura.skills.SkillsAttachments", "PLAYER_SKILLS", "adminResetAll");
            resetAttachment(player, "net.revilodev.aura.abilities.AbilitiesAttachments", "PLAYER_ABILITIES", "adminReset");
            sync(player, "net.revilodev.aura.skills.SkillsNetwork");
            sync(player, "net.revilodev.aura.abilities.AbilitiesNetwork");
        } catch (ReflectiveOperationException | ClassCastException exception) {
            GatewayExpansion.LOGGER.warn("Unable to reset Aura skills and abilities for hard dungeon death", exception);
        }
    }

    private static void resetAttachment(ServerPlayer player, String className, String fieldName, String resetMethod)
            throws ReflectiveOperationException {
        Class<?> attachmentsClass = Class.forName(className);
        Field field = attachmentsClass.getField(fieldName);
        Supplier<?> supplier = (Supplier<?>) field.get(null);
        AttachmentType<?> attachmentType = (AttachmentType<?>) supplier.get();
        Object progression = player.getData(attachmentType);
        progression.getClass().getMethod(resetMethod).invoke(progression);
    }

    private static void sync(ServerPlayer player, String className) throws ReflectiveOperationException {
        Class<?> networkClass = Class.forName(className);
        Method syncMethod = networkClass.getMethod("syncTo", ServerPlayer.class);
        syncMethod.invoke(null, player);
    }
}
