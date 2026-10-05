package sigf.xo.mod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import sigf.xo.Xo;

/**
 * Mission "Nether portal on Mount Chiliad": Steve stacks an obsidian frame on the summit, lights it with flint and steel,
 * and steps in. The bridge's Nether turns the mountain to hell: netherrack, fire and lava rivers running downhill,
 * a red GTA sky, and anything standing in it burns.
 */
final class Chiliad {
	/** Summit, GTA coordinates; Steve faces south (GTA heading 180 = Minecraft +z) over the valley. */
	private static final double GX = 501.0, GY = 5594.0, GZ = 797.0;
	private static final float HEAD = 180f;
	private static final int AHEAD = 7;
	/** Minecraft stops at y 319: the summit (GTA z 795) is lowered by this much while Steve is up there. */
	static final int SHIFT = -600;

	private static BlockPos base;

	/** Pilot "c5prep": Steve on the summit, hell put back. */
	static void prep() {
		Xo.command("gamemode creative @p");
		Xo.command("effect clear @p");
		dev.rehan.passthrough.Nether.stop();
		Xo.gta("yshift", "v", 0);
		Xo.gta("hell", "on", 0);
		Xo.gta("view", "mode", 1);
		Xo.gta("weather", "w", "EXTRASUNNY");
		Xo.gta("time", "h", 18, "m", 20);
		// the story's streaming and Minecraft's server dislike one huge jump: hop in steps
		double[][] hops = {{300, 1400, 70}, {400, 2800, 100}, {450, 4200, 150}, {480, 5300, 300}};
		var g = Xo.gtaPlayer();
		double t = 0;
		if (g == null || Math.abs(g.z) < 4000) {
			for (double[] h : hops) {
				final double[] hh = h;
				Xo.after(t, () -> Xo.teleportGtaWorld(hh[0], hh[1], hh[2], HEAD));
				t += 4;
			}
		}

		Xo.after(t, () -> {
			Xo.gta("yshift", "v", SHIFT);
			Xo.teleportGtaWorld(GX, GY, GZ, HEAD);
		});
		Xo.after(t + 4, () -> {
			ServerPlayer p = Xo.player();
			if (p == null) {
				return;
			}

			base = p.blockPosition();
			Xo.log("c5 base " + base);
		});
	}

	static final int PREP_SECONDS = 24;

	/** Pilot "c5go": about 20 s. */
	static void go() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		ServerLevel level = Xo.level();
		BlockPos feet = p.blockPosition();
		// the first solid block under Steve is the summit's ground
		BlockPos ground = feet;
		for (int dy = 0; dy > -8; dy--) {
			if (!level.getBlockState(feet.offset(0, dy - 1, 0)).isAir()) {
				ground = feet.offset(0, dy, 0);
				break;
			}
		}

		base = ground;
		Xo.title("NETHER PORTAL", "Mount Chiliad", 2.2);
		Xo.command("item replace entity @p weapon.mainhand with minecraft:obsidian 64");

		// frame: 4 wide (lateral -2..1), 5 tall, AHEAD blocks in front of Steve, plane spans x
		List<BlockPos> frame = new ArrayList<>();
		for (int a = -2; a <= 1; a++) {
			frame.add(at(AHEAD, -1, a));
		}

		for (int h = 0; h <= 2; h++) {
			frame.add(at(AHEAD, h, -2));
			frame.add(at(AHEAD, h, 1));
		}

		for (int a = -2; a <= 1; a++) {
			frame.add(at(AHEAD, 3, a));
		}

		// a flat pad so the frame never floats on the slope
		for (int d = AHEAD - 1; d <= AHEAD + 1; d++) {
			for (int a = -3; a <= 2; a++) {
				for (int dy = -1; dy >= -3; dy--) {
					BlockPos pos = at(d, dy, a);
					if (level.getBlockState(pos).isAir()) {
						level.setBlockAndUpdate(pos, Blocks.BLACKSTONE.defaultBlockState());
					}
				}
			}
		}

		Xo.gta("xo.text", "text", "Building a ~p~Nether portal~s~ on Mount Chiliad", "ms", 4000);
		for (int i = 0; i < frame.size(); i++) {
			final BlockPos pos = frame.get(i);
			Xo.after(0.7 + i * 0.33, () -> {
				p.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
				level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
				Vec3 c = Vec3.atCenterOf(pos);
				Xo.sound(SoundEvents.STONE_PLACE, c, 1.3f, 0.7f);
				Xo.particles(ParticleTypes.SMOKE, c, 5, 0.3);
			});
		}

		double lit = 0.7 + frame.size() * 0.33 + 0.8;
		Xo.after(lit - 0.5, () -> Xo.command("item replace entity @p weapon.mainhand with minecraft:flint_and_steel"));
		Xo.after(lit, () -> {
			p.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
			BlockPos inside = at(AHEAD, 0, 0);
			level.setBlockAndUpdate(inside, Blocks.FIRE.defaultBlockState());
			Xo.sound(SoundEvents.FLINTANDSTEEL_USE, Vec3.atCenterOf(inside), 1.5f, 1f);
			Xo.particles(ParticleTypes.FLAME, Vec3.atCenterOf(inside), 25, 0.4);
			Xo.text("The frame ~p~lights up", 3);
		});
		Xo.after(lit + 1.0, () -> {
			Xo.sound(SoundEvents.PORTAL_AMBIENT, Vec3.atCenterOf(at(AHEAD, 1, 0)), 1.5f, 1f);
			Xo.particles(ParticleTypes.PORTAL, Vec3.atCenterOf(at(AHEAD, 1, 0)), 60, 1.2);
		});

		// Steve steps into the portal: the bridge starts the Nether, the mountain turns to hell
		Xo.after(lit + 1.8, () -> {
			double[] g = Xo.toGta(Vec3.atBottomCenterOf(at(AHEAD, 0, 0)).add(-0.5, 0, 0));
			Xo.gta("walk", "x", g[0], "y", g[1], "z", g[2], "speed", 1.6, "timeout", 6000);
			Xo.text("~r~Hell~s~ pours down Mount Chiliad", 6);
		});
		Xo.after(lit + 10.0, () -> Xo.title("HELL ON CHILIAD", "Lava runs down the mountain", 3));
		// and back out to watch the lava run
		Xo.after(lit + 6.2, () -> {
			double[] g = Xo.toGta(Vec3.atBottomCenterOf(at(2, 0, 0)));
			Xo.gta("walk", "x", g[0], "y", g[1], "z", g[2], "speed", 1.4, "timeout", 4000);
		});
	}

	/** The block d ahead of Steve's start, dy up, a to his left (south: left is +x... see FX/LX). */
	private static BlockPos at(final int d, final int dy, final int a) {
		// facing south (+z): forward (0,+1), left (+1,0)
		return base.offset(a, dy, d);
	}
}
