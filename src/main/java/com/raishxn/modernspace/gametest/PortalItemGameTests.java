package com.raishxn.modernspace.gametest;

import com.raishxn.modernspace.ModernSpace;
import com.raishxn.modernspace.common.PersonalSpacePortalRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ModernSpace.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortalItemGameTests {

    private PortalItemGameTests() {}

    /** {@code /give} (and quest rewards) put the portal in the inventory and must not leave a second, real copy on the ground. */
    @GameTest(template = "qa_empty", batch = "give", timeoutTicks = 40)
    public static void giveLeavesNoExtraPortal(GameTestHelper helper) {
        // Same steps as vanilla GiveCommand (the mock player of 1.20.1 GameTests has no network channel).
        ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        player.getInventory().clearContent();
        ItemStack stack = new ItemStack(PersonalSpacePortalRegistry.PORTAL_ITEM.get());
        if (player.getInventory().add(stack) && stack.isEmpty()) {
            stack.setCount(1);
            ItemEntity fake = player.drop(stack, false);
            if (fake != null) fake.makeFakeItem();
        }
        helper.runAfterDelay(1, () -> {
            int held = player.getInventory().countItem(PersonalSpacePortalRegistry.PORTAL_ITEM.get());
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(8),
                    e -> e.getItem().is(PersonalSpacePortalRegistry.PORTAL_ITEM.get()));
            helper.assertTrue(held == 1, "expected 1 portal in the inventory, got " + held);
            // Only the fake copy may exist: never picked up and expiring on its next tick.
            for (ItemEntity e : dropped) {
                helper.assertTrue(e.getAge() >= e.lifespan - 1 && e.hasPickUpDelay(),
                        "a dropped portal is a real item (age " + e.getAge() + ", lifespan " + e.lifespan + ")");
            }
            helper.succeed();
        });
    }

    /** A dropped portal survives fire and explosions. */
    @GameTest(template = "qa_empty", batch = "drop", timeoutTicks = 20)
    public static void droppedPortalIsProtected(GameTestHelper helper) {
        ItemEntity item = helper.spawnItem(PersonalSpacePortalRegistry.PORTAL_ITEM.get(), 2, 2, 2);
        helper.assertTrue(item.isInvulnerable(), "dropped portal should be invulnerable");
        helper.assertTrue(item.fireImmune(), "dropped portal should be fire immune");
        helper.succeed();
    }
}
