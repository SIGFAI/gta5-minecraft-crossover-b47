package sigf.xo.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import sigf.xo.Xo;

/** Dev-only helper: runs the lines of gen/pilot.txt (tp x y z h | gta op k=v,k=v | mc command). Removed before shipping. */
final class DevPilot {
	private static final Path FILE = Path.of("C:/mod/work/b47_1514/gen/pilot.txt");

	static void start() {
		Xo.every(0.5, () -> {
			try {
				if (!Files.exists(FILE)) {
					return;
				}

				List<String> lines = Files.readAllLines(FILE);
				Files.delete(FILE);
				for (String l : lines) {
					run(l.trim());
				}
			} catch (Exception e) {
				Xo.log("pilot: " + e);
			}
		});
	}

	private static void run(final String l) {
		if (l.equals("c5prep")) {
			Chiliad.prep();
		} else if (l.equals("c5go")) {
			Chiliad.go();
		} else if (l.equals("prep")) {
			XoMod.prep();
		} else if (l.equals("go")) {
			XoMod.go();
		} else if (l.startsWith("tp ")) {
			String[] a = l.substring(3).trim().split("\\s+");
			Xo.teleportGtaWorld(Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Float.parseFloat(a[3]));
		} else if (l.startsWith("mc ")) {
			Xo.command(l.substring(3));
		} else if (l.startsWith("gta ")) {
			String[] a = l.substring(4).trim().split("\\s+", 2);
			Object[] kv = new Object[0];
			if (a.length > 1) {
				String[] parts = a[1].split(",");
				kv = new Object[parts.length * 2];
				for (int i = 0; i < parts.length; i++) {
					String[] p = parts[i].split("=", 2);
					kv[i * 2] = p[0];
					try {
						kv[i * 2 + 1] = Double.parseDouble(p[1]);
					} catch (NumberFormatException e) {
						kv[i * 2 + 1] = p[1];
					}
				}
			}

			Xo.gta(a[0], kv);
		} else if (l.startsWith("pos")) {
			var p = Xo.player();
			Xo.log("PILOT pos " + (p == null ? "none" : p.position()) + " gta " + Xo.gtaPlayer() + " yoff " + Xo.yOffset());
		} else if (l.startsWith("blocks ")) {
			// blocks x y z r : counts non-air blocks in a cube around mc position
			String[] a = l.substring(7).trim().split("\\s+");
			int x = Integer.parseInt(a[0]), y = Integer.parseInt(a[1]), z = Integer.parseInt(a[2]), r = Integer.parseInt(a[3]);
			var level = Xo.level();
			StringBuilder sb = new StringBuilder("PILOT blocks");
			for (int yy = y + r; yy >= y - r; yy--) {
				sb.append("\n y=").append(yy).append(": ");
				for (int zz = z - r; zz <= z + r; zz++) {
					for (int xx = x - r; xx <= x + r; xx++) {
						sb.append(level.getBlockState(new net.minecraft.core.BlockPos(xx, yy, zz)).isAir() ? '.' : '#');
					}
					sb.append('|');
				}
			}

			Xo.log(sb.toString());
		}
	}
}
