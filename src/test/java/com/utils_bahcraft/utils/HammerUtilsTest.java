package com.utils_bahcraft.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HammerUtilsTest {

    private RecordingWorldActions recorder;

    @BeforeEach
    public void setUp() {
        recorder = new RecordingWorldActions();
        HammerUtils.setWorldActions(recorder);
    }

    @AfterEach
    public void tearDown() {
        HammerUtils.setWorldActions(new DefaultWorldActions());
    }

    @Test
    public void testSpawnLightningDelegates() {
        // Use null for Level to avoid mocking final Minecraft classes in unit tests
        Level level = null;
        Vec3 pos = new Vec3(1.0, 2.0, 3.0);

        HammerUtils.spawnLightningAt(level, pos);

        assertFalse(recorder.calls.isEmpty());
        assertEquals("spawnLightningAt", recorder.calls.get(0).name);
        assertSame(level, recorder.calls.get(0).args[0]);
        assertSame(pos, recorder.calls.get(0).args[1]);
        assertEquals(false, recorder.calls.get(0).args[2]);
    }

    @Test
    public void testSpawnThunderDelegates() {
        Level level = null;
        Vec3 pos = new Vec3(4.0, 5.0, 6.0);

        HammerUtils.spawnThunderAt(level, pos, 0.8f, 1.2f);

        assertFalse(recorder.calls.isEmpty());
        assertEquals("spawnThunderAt", recorder.calls.get(0).name);
        assertSame(level, recorder.calls.get(0).args[0]);
        assertSame(pos, recorder.calls.get(0).args[1]);
        assertEquals(0.8f, recorder.calls.get(0).args[2]);
        assertEquals(1.2f, recorder.calls.get(0).args[3]);
    }

    @Test
    public void testStrikeBlockDelegates() {
        Level level = null;
        BlockPos pos = new BlockPos(10, 64, 10);

        HammerUtils.strikeBlockWithLightning(level, pos, true);

        assertFalse(recorder.calls.isEmpty());
        assertEquals("strikeBlockWithLightning", recorder.calls.get(0).name);
        assertSame(level, recorder.calls.get(0).args[0]);
        assertSame(pos, recorder.calls.get(0).args[1]);
        assertEquals(true, recorder.calls.get(0).args[2]);
    }

    @Test
    public void testForceKillDelegates() {
        Level level = null;
        LivingEntity target = null;
        DamageSource ds = null;

        HammerUtils.forceKill(target, level, ds);

        assertFalse(recorder.calls.isEmpty());
        assertEquals("forceKill", recorder.calls.get(0).name);
        assertSame(target, recorder.calls.get(0).args[0]);
        assertSame(level, recorder.calls.get(0).args[1]);
        assertSame(ds, recorder.calls.get(0).args[2]);
    }
}