// Minecraft Crossover, GTA side: reports every GTA person who dies near the player ("death"); Minecraft raises a husk there.
// Runs inside rehan's script.cpp (anonymous namespace): natives::*, xon::*, xo_* bricks, WAIT, worldGetAllPeds.

	std::unordered_set<Ped> g_reportedDead;
	int g_nextDeathScan = 0;

	void mod_connected(Ped player)
	{
		(void)player;
		g_reportedDead.clear();
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
			g_reportedDead.clear();
	}

	bool mod_op(Ped player, const std::string &op, const std::string &m)
	{
		(void)player;
		if (op == "yshift")
		{
			g_yShift = float(json_num(m, "v", 0.0));
			g_haveOffset = false; // re-level Minecraft's ground with the new shift
			return true;
		}
		return false;
	}
