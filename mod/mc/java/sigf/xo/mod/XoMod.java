package sigf.xo.mod;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import sigf.xo.Xo;

/**
 * Minecraft Crossover, mission "Diamonds in the vault": the Pacific Standard bank's vault corridor is packed with
 * Minecraft diamond ore. Steve walks in with a pickaxe and mines a tunnel, every ore breaks with Minecraft's crack
 * animation and sound, drops a diamond, and Los Santos answers with GTA cash, gold and banknote bursts.
 */
public final class XoMod implements ModInitializer {
	/** Vault hallway, GTA coordinates: Steve stands here facing west (-x in Minecraft; GTA heading 90). */
	private static final double GX = 256.0, GY = 225.0, GZ = 101.8;
	private static final int DEPTH0 = 3, DEPTH1 = 7, HALF = 3, HEIGHT = 5;
	/** GTA heading Steve faces; forward and lateral steps in Minecraft blocks. */
	private static final float HEAD = 90f;
	private static final int FX = (int) Math.round(-Math.sin(Math.toRadians(180 - HEAD))), FZ = (int) Math.round(Math.cos(Math.toRadians(180 - HEAD)));
	private static final int LX = -FZ, LZ = FX;

	private static final List<BlockPos> tunnel = new ArrayList<>();
	private static BlockPos base;
	private static int mined;

	@Override
	public void onInitialize() {
		DevPilot.start();
		Xo.demo(0, () -> {
			Xo.teleportGtaWorld(195.0, -933.0, 30.7, 140f);
			Xo.gta("time", "h", 12, "m", 0);
			Xo.gta("weather", "w", "EXTRASUNNY");
			Xo.gta("view", "mode", 1);
		});
		Xo.demo(4, XoMod::town);
		Xo.demo(14, XoMod::invasion);
		Xo.demo(26, XoMod::creeperCar);
		Xo.demo(31, () -> {
			dev.rehan.passthrough.MobWar.clearMobs();
			prep();
		});
		Xo.demo(36, XoMod::go);
	}

	private static void walkTo(final double gx, final double gy, final double gz) {
		Xo.gta("walk", "x", gx, "y", gy, "z", gz, "speed", 1.6, "timeout", 6000);
	}

	/** Scene 1: a Minecraft house and farm animals appear on the plaza, GTA pedestrians stroll past. */
	private static void town() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		Xo.text("A ~g~Minecraft~s~ house rises in ~y~Los Santos", 4);
		Vec3 look = p.getLookAngle();
		double len = Math.max(0.01, Math.sqrt(look.x * look.x + look.z * look.z));
		BlockPos column = BlockPos.containing(p.getX() + look.x / len * 10, p.getY(), p.getZ() + look.z / len * 10);
		BlockPos o = groundAbove(Xo.level(), column);
		if (o == null) {
			return;
		}

		ServerLevel level = Xo.level();
		for (int y = 0; y <= 5; y++) {
			final int yy = y;
			Xo.after(yy * 0.6, () -> {
				for (int x = 0; x < 6; x++) {
					for (int z = 0; z < 5; z++) {
						boolean wall = x == 0 || x == 5 || z == 0 || z == 4;
						Block b = null;
						if (yy == 0) {
							b = Blocks.COBBLESTONE;
						} else if (yy == 5) {
							b = Blocks.SPRUCE_PLANKS;
						} else if (wall && !(z == 0 && x == 2 && yy <= 2)) {
							b = (yy == 2 && (x == 1 || x == 4) && z == 0) ? Blocks.GLASS : Blocks.OAK_PLANKS;
						}

						if (b != null) {
							level.setBlockAndUpdate(o.offset(x, yy, z), b.defaultBlockState());
						}
					}
				}

				Vec3 c = Vec3.atBottomCenterOf(o).add(3, yy + 1, 2.5);
				Xo.sound(SoundEvents.WOOD_PLACE, c, 1.4f, 0.8f + 0.05f * yy);
				Xo.particles(ParticleTypes.CLOUD, c, 14, 2.5);
			});
		}

		Xo.after(3.5, () -> {
			Vec3 c = Vec3.atBottomCenterOf(o).add(2.5, 1, -3);
			Xo.spawn(EntityTypes.PIG, c);
			Xo.spawn(EntityTypes.PIG, c.add(1.5, 0, 0));
			Xo.spawn(EntityTypes.SHEEP, c.add(3, 0, -1));
			Xo.spawn(EntityTypes.COW, c.add(-2, 0, -1));
			Xo.spawn(EntityTypes.CHICKEN, c.add(0, 0, 2));
			Xo.spawn(EntityTypes.IRON_GOLEM, c.add(5, 0, 1));
			Xo.particles(ParticleTypes.HAPPY_VILLAGER, c.add(0, 1, 0), 30, 3);
			for (int i = 0; i < 4; i++) {
				Xo.gta("xo.ped", "id", "walker" + i, "model", "a_m_y_hipster_01", "ahead", 6 + i * 2, "task", "wander");
			}
		});
		Xo.after(2, () -> walkTo(205.0, -925.0, 30.7));
		Xo.after(6, () -> walkTo(190.0, -915.0, 30.7));
	}

	/** Scene 2: husks, zombies and creepers pour in; they hunt GTA's people and chase Steve. */
	private static void invasion() {
		Xo.text("~r~Mobs~s~ invade ~y~Los Santos", 4);
		Xo.command("gamemode survival @p");
		Xo.command("effect give @p minecraft:resistance infinite 5 true");
		Xo.command("effect give @p minecraft:regeneration infinite 3 true");
		Xo.gta("time", "h", 20, "m", 30);
		dev.rehan.passthrough.MobWar.spawn("husk", 3, 9, 14, 140, 0, null);
		dev.rehan.passthrough.MobWar.spawn("zombie", 3, 10, 16, 140, 0, null);
		dev.rehan.passthrough.MobWar.spawn("creeper", 3, 9, 13, 140, 0, null);
		for (int i = 0; i < 4; i++) {
			Xo.gta("xo.ped", "id", "victim" + i, "model", "a_m_y_beach_01", "ahead", 8 + i, "task", "flee");
		}

		Xo.after(3, () -> walkTo(195.0, -905.0, 30.7));
		Xo.after(7, () -> walkTo(215.0, -930.0, 30.7));
		Xo.every(0.5, () -> {
			// creepers that get close to Steve light their fuse: Minecraft blast = GTA explosion
			ServerPlayer p = Xo.player();
			if (p == null) {
				return;
			}

			for (var e : Xo.near(p.position(), 4, x -> x.getType() == EntityTypes.CREEPER)) {
				if (e instanceof net.minecraft.world.entity.monster.Creeper c) {
					c.ignite();
				}
			}
		});
	}

	/** Scene 3: a sports car is parked in front of Steve; a creeper walks up to it. */
	private static void creeperCar() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		Xo.text("A creeper finds a ~y~sports car", 4);
		Xo.gta("time", "h", 12, "m", 0);
		Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
		Vec3 carAt = p.position().add(look.scale(8));
		Xo.car("bait", "adder", carAt, Xo.heading(p.getYRot()) + 90f, 0);
		Xo.after(1, () -> Xo.spawn(EntityTypes.CREEPER, carAt.add(look.scale(5))));
		Xo.every(0.5, () -> {
			for (var e : Xo.near(carAt, 3.5, x -> x.getType() == EntityTypes.CREEPER)) {
				if (e instanceof net.minecraft.world.entity.monster.Creeper c) {
					c.ignite();
				}
			}
		});
	}

	/** Part 1: Steve in the vault hallway, the old ore cleared away. Called at demo start, or with the pilot line "prep". */
	static void prep() {
		Xo.command("gamemode creative @p");
		Xo.command("effect clear @p");
		Xo.teleportGtaWorld(GX, GY, GZ, HEAD);
		Xo.gta("time", "h", 13, "m", 0);
		Xo.gta("view", "mode", 1);
		Xo.after(3, () -> {
			ServerPlayer p = Xo.player();
			if (p == null) {
				return;
			}

			ServerLevel level = Xo.level();
			base = groundAbove(level, p.blockPosition());
			if (base == null) {
				base = p.blockPosition();
			}

			for (int d = 0; d <= DEPTH1 + 1; d++) {
				for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
					for (int dy = 0; dy < HEIGHT + 1; dy++) {
						level.setBlockAndUpdate(at(d, dy, dz), Blocks.AIR.defaultBlockState());
					}
				}
			}

			Xo.command("item replace entity @p weapon.mainhand with minecraft:diamond_pickaxe");
		});
	}

	/** Part 2, about 19 s: the ore rises and Steve mines it. Pilot line "go". */
	static void go() {
		Xo.title("DIAMOND VAULT", "Minecraft x Los Santos", 2.2);
		setup();
	}

	private static void setup() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		ServerLevel level = Xo.level();
		if (base == null) {
			base = p.blockPosition();
		}

		mined = 0;
		tunnel.clear();
		Xo.text("~y~Pacific Standard Bank~s~: the vault is full of ~b~diamond ore", 4);

		// the ore rises slice by slice
		for (int dx = DEPTH0; dx <= DEPTH1; dx++) {
			final int d = dx;
			Xo.after(0.3 + (d - DEPTH0) * 0.25, () -> {
				for (int dz = -HALF; dz <= HALF; dz++) {
					for (int dy = 0; dy < HEIGHT; dy++) {
						double r = level.getRandom().nextDouble();
						Block b = r < 0.6 ? Blocks.DIAMOND_ORE : r < 0.9 ? Blocks.DEEPSLATE_DIAMOND_ORE : r < 0.94 ? Blocks.GOLD_BLOCK : Blocks.GLOWSTONE;
						level.setBlockAndUpdate(at(d, dy, dz), b.defaultBlockState());
					}
				}

				Vec3 c = Vec3.atBottomCenterOf(at(d, 0, 0)).add(0, 1.5, 0);
				Xo.sound(SoundEvents.AMETHYST_BLOCK_CHIME, c, 1.2f, 0.7f + 0.1f * (d - DEPTH0));
				Xo.particles(ParticleTypes.END_ROD, c, 8, 1.2);
			});
		}

		// mining order: two layers, three columns wide, two blocks high (top block first)
		for (int dx = DEPTH0; dx <= DEPTH0 + 1; dx++) {
			for (int dz : new int[] {0, -1, 1}) {
				tunnel.add(at(dx, 1, dz));
				tunnel.add(at(dx, 0, dz));
			}
		}

		while (tunnel.size() > 8) {
			tunnel.removeLast();
		}

		for (int i = 0; i < tunnel.size(); i++) {
			final int k = i;
			Xo.after(3.2 + i * 0.95, () -> mine(k));
		}

		Xo.after(3.2 + tunnel.size() * 0.95 + 0.6, XoMod::finale);
	}

	/** Cracks tunnel block k in four stages with arm swings, then breaks it and pays out. */
	private static void mine(final int k) {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		BlockPos pos = tunnel.get(k);
		ServerLevel level = Xo.level();
		if (k == 0 || k == 6) {
			// step up to the face: one block closer per layer
			double[] g = Xo.toGta(Vec3.atBottomCenterOf(base).add(FX * (k == 0 ? 1.2 : 2.2), 0, FZ * (k == 0 ? 1.2 : 2.2)));
			Xo.gta("walk", "x", g[0], "y", g[1], "z", g[2], "speed", 1.2, "timeout", 2500);
		}

		for (int s = 0; s < 4; s++) {
			final int stage = s * 2 + 1;
			Xo.after(s * 0.2, () -> {
				p.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				level.destroyBlockProgress(-77, pos, stage);
				Xo.sound(SoundEvents.STONE_HIT, Vec3.atCenterOf(pos), 1f, 0.9f);
			});
		}

		Xo.after(0.85, () -> {
			p.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			level.destroyBlockProgress(-77, pos, -1);
			level.destroyBlock(pos, false);
			payout(pos);
		});
	}

	private static void payout(final BlockPos pos) {
		mined++;
		Vec3 c = Vec3.atCenterOf(pos);
		Xo.command("give @p minecraft:diamond");
		Xo.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, c, 0.8f, 1.0f + 0.04f * mined);
		Xo.particles(ParticleTypes.HAPPY_VILLAGER, c, 10, 0.5);
		Xo.ptfx("core", "ent_brk_banknotes", c, 1.0);
		Xo.prop("loot" + mined, mined % 3 == 0 ? "prop_money_bag_01" : "prop_cash_pile_01", c.add(0, 0.3, 0), mined * 37f, false, false, 255);
		Xo.text("Diamonds: ~b~" + mined, 2);
	}

	private static void finale() {
		ServerPlayer p = Xo.player();
		if (p == null) {
			return;
		}

		Xo.title("VAULT CLEARED", mined + " diamonds. Los Santos pays.", 3);
		for (int i = 0; i < 8; i++) {
			final int k = i;
			Xo.after(i * 0.15, () -> Xo.prop("rain" + k, "prop_money_bag_01", p.position().add(Math.cos(k) * 1.5, 3 + k % 3, Math.sin(k) * 1.5), k * 45f, false, true, 255));
		}

		Xo.ptfx("core", "ent_brk_banknotes", p.position().add(0, 2, 0), 2.0);
	}

	/** The block d ahead of Steve's start, dy up, dz to his left. */
	private static BlockPos at(final int d, final int dy, final int dz) {
		return base.offset(FX * d + LX * dz, dy, FZ * d + LZ * dz);
	}

	private static BlockPos groundAbove(final ServerLevel level, final BlockPos column) {
		for (int dy = 4; dy >= -8; dy--) {
			BlockPos below = column.offset(0, dy - 1, 0);
			if (!level.getBlockState(below).isAir() && level.getBlockState(below.above()).isAir()) {
				return below.above();
			}
		}

		return null;
	}
}
