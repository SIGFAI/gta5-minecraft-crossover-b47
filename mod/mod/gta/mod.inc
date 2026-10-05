// Example crossover mod, GTA side ("Diamond Bounty"): Minecraft drives most of it through the kit's xo.* bricks;
// this file adds the two things only GTA can do.
//   1. GTA -> Minecraft: every GTA person who dies near the player is reported ("death"); Minecraft raises a husk there.
//   2. A mod op Minecraft calls by name: Xo.gta("bounty.rain", "n", 6) drops loose money bags around the player.
// Runs inside rehan's script.cpp (anonymous namespace): natives::*, xon::*, xo_* bricks, WAIT, worldGetAllPeds.

	std::unordered_set<Ped> g_reportedDead;
	int g_nextDeathScan = 0;

	void mod_connected(Ped player)
	{
		(void)player;
		g_reportedDead.clear();
		xo_text("~g~Minecraft~s~ x ~y~Los Santos~s~: Diamond Bounty", 5000);
	}

	void mod_tick(Ped player)
	{
		const int now = natives::GetGameTimer();
		if (now < g_nextDeathScan)
			return;
		g_nextDeathScan = now + 500;
		const Vector3 me = natives::GetEntityCoords(player, TRUE);
		int peds[256];
		const int n = worldGetAllPeds(peds, 256);
		for (int i = 0; i < n; ++i)
		{
			const Ped p = peds[i];
			if (p == player || g_doublePeds.count(p) || g_reportedDead.count(p) || !natives::IsPedDeadOrDying(p))
				continue;
			const Vector3 at = natives::GetEntityCoords(p, FALSE);
			const float dx = at.x - me.x, dy = at.y - me.y;
			if (dx * dx + dy * dy > 60.0f * 60.0f)
				continue;
			g_reportedDead.insert(p);
			char mc[96];
			xo_to_mc_json(at, mc, sizeof(mc));
			xo_emit("death", "\"mc\":%s", mc);
		}
		if (g_reportedDead.size() > 512)
			g_reportedDead.clear(); // old bodies are long gone; a re-report is harmless
	}

	bool mod_op(Ped player, const std::string &op, const std::string &m)
	{
		if (op != "bounty.rain")
			return false;
		const int count = std::clamp(int(json_num(m, "n", 6)), 1, 12);
		const Vector3 me = natives::GetEntityCoords(player, TRUE);
		for (int i = 0; i < count; ++i)
		{
			const float a = 6.2831853f * float(i) / float(count);
			Vector3 at = me;
			at.x += std::cos(a) * 3.0f;
			at.y += std::sin(a) * 3.0f;
			at.z += 6.0f + float(i % 3);
			// loose (not frozen): GTA's physics drops them; ids rain0..rainN replace the previous shower
			xo_spawn_prop("rain" + std::to_string(i), "prop_money_bag_01", at, a * 57.29578f, false, true, 255);
		}
		xo_sound("PICK_UP", "HUD_FRONTEND_DEFAULT_SOUNDSET", nullptr);
		return true;
	}
