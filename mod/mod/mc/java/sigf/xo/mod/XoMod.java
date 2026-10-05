package sigf.xo.mod;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import sigf.xo.Xo;

/**
 * Example crossover mod, Minecraft side: "Diamond Bounty".
 * - Minecraft -> GTA: diamond blocks appear around the player; each one is mirrored in GTA as a money bag (Xo.mirrorBlock).
 *   Walk onto one: it is collected in both games (GTA sound + particles, Minecraft particles), a counter shows in GTA.
 *   All five: Xo.gta("bounty.rain") runs the GTA-side op from mod.inc (money bags rain down), then a new round.
 * - GTA -> Minecraft: mod.inc reports every GTA person who dies near the player (xo_emit("death")); a husk rises there.
 * - The demo (play.ps1 -Demo) drives both games from here: teleport, time, weather, peds, an explosion.
 */
public final class XoMod implements ModInitializer {
	private static final int BOUNTIES = 5;
	private static final List<BlockPos> bounty = new ArrayList<>();
	private static int collected;

	@Override
	public void onInitialize() {
		Xo.onLink(() -> {
			Xo.text("~b~Diamond Bounty~s~: walk onto the ~g~money bags", 5);
			Xo.after(3, XoMod::newRound);
		});

		// GTA's dead rise in Minecraft
		Xo.on("death", m -> {
			var p = m.getAsJsonArray("mc");
			Vec3 at = new Vec3(p.get(0).getAsDouble(), p.get(1).getAsDouble() + 0.2, p.get(2).getAsDouble());
			var husk = Xo.spawn(EntityTypes.HUSK, at);
			Xo.particles(ParticleTypes.SOUL, at.add(0, 1, 0), 30, 0.6);
			Xo.sound(SoundEvents.ZOMBIE_AMBIENT, at, 1f, 0.7f);
			Xo.log("a GTA death rose as a husk" + (husk == null ? " (spawn failed)" : ""));
		});

		Xo.every(0.25, XoMod::checkPickup);

		// Demo (75 s recording): Legion Square at noon, the bounty, people, a blast.
		Xo.demo(0, () -> {
			Xo.teleportGtaWorld(195.0, -933.0, 30.7, 140f);
			Xo.gta("time", "h", 12, "m", 0);
			Xo.gta("weather", "w", "EXTRASUNNY");
		});
		Xo.demo(4, XoMod::newRound);
		Xo.demo(10, () -> {
			for (int i = 0; i < 4; i++) {
				Xo.gta("xo.ped", "id", "walker" + i, "model", "a_m_y_hipster_01", "ahead", 8 + i * 2, "task", "wander");
			}
		});
		Xo.demo(20, () -> {
			ServerPlayer p = Xo.player();
			if (p != null) {
				Xo.boom(p.position().add(p.getLookAngle().multiply(10, 0, 10)), 2, 1.0);
			}
		});
		Xo.demo(30, () -> walkTo(0));
		Xo.demo(40, () -> walkTo(1));
		Xo.demo(50, () -> Xo.gta("bounty.rain", "n", 8));
	}

	/** Five diamond blocks on the ground around the player, each mirrored in GTA as a money bag. */
	private static void newRound() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		ServerLevel level = Xo.level();
		clearRound(level);
		collected = 0;
		var random = level.getRandom();
		for (int i = 0; i < BOUNTIES; i++) {
			double a = Math.PI * 2 * i / BOUNTIES + random.nextDouble() * 0.5;
			double d = 6 + random.nextDouble() * 6;
			BlockPos column = BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d);
			BlockPos pos = groundAbove(level, column);
			if (pos == null) {
				continue;
			}

			level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
			Xo.mirrorBlock(pos.above(), "prop_money_bag_01");
			bounty.add(pos);
		}

		Xo.text("Bounty: ~g~0/" + bounty.size(), 3);
	}

	/** The air block on top of the ground in this column (rehan's barriers are GTA's ground), near the player's height. */
	private static BlockPos groundAbove(final ServerLevel level, final BlockPos column) {
		for (int dy = 4; dy >= -8; dy--) {
			BlockPos below = column.offset(0, dy - 1, 0);
			if (!level.getBlockState(below).isAir() && level.getBlockState(below.above()).isAir()) {
				return below.above();
			}
		}

		return null;
	}

	private static void clearRound(final ServerLevel level) {
		for (BlockPos pos : bounty) {
			if (level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}

			Xo.unmirrorBlock(pos.above());
		}

		bounty.clear();
	}

	private static void checkPickup() {
		ServerPlayer p = Xo.player();
		if (p == null || bounty.isEmpty()) {
			return;
		}

		ServerLevel level = Xo.level();
		for (int i = 0; i < bounty.size(); i++) {
			BlockPos pos = bounty.get(i);
			Vec3 c = Vec3.atBottomCenterOf(pos.above());
			if (p.position().distanceTo(c) > 1.8) {
				continue;
			}

			bounty.remove(i);
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			Xo.unmirrorBlock(pos.above());
			Xo.gtaSound("PICK_UP", "HUD_FRONTEND_DEFAULT_SOUNDSET");
			Xo.ptfx("scr_rcbarry2", "scr_clown_appears", c, 1.0);
			Xo.particles(ParticleTypes.HAPPY_VILLAGER, c.add(0, 0.5, 0), 25, 0.5);
			collected++;
			Xo.text("Bounty: ~g~" + collected + "/" + (collected + bounty.size()), 3);
			if (bounty.isEmpty()) {
				Xo.gta("bounty.rain", "n", 8);
				Xo.title("BOUNTY!", "Los Santos pays in diamonds", 3);
				Xo.after(10, XoMod::newRound);
			}

			return;
		}
	}

	/** Demo: GTA's player walks to bounty i (rehan's "walk" op takes GTA coordinates). */
	private static void walkTo(final int i) {
		if (i >= bounty.size()) {
			return;
		}

		double[] g = Xo.toGta(Vec3.atBottomCenterOf(bounty.get(i).above()));
		Xo.gta("walk", "x", g[0], "y", g[1], "z", g[2], "speed", 1.5, "timeout", 9000);
	}
}
