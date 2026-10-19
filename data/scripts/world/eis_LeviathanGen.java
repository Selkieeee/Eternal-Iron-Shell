package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Random;

// Leviathan system. Called from eis_modPlugin.onGameLoad, so it is added to existing saves as well as new games.
public class eis_LeviathanGen {

    // Lookup id for getStarSystem(). A star system's own getId() is always its name in lowercase ("leviathan"),
    // so the unique id is what keeps our lookups from matching another mod's Leviathan.
    public static final String SYSTEM_ID = "eis_leviathan";
    public static final String STAR_ID = "eis_leviathan_star";

    private static final Color LEVIATHAN_PINK = new Color(237, 174, 192);
    // System light: a soft pink, brighter than LEVIATHAN_PINK so planets aren't darkened as much
    private static final Color LEVIATHAN_LIGHT = new Color(255, 225, 235);

    // Planet 2 (tundra) sits in a cleared gap in the dust disk, so the disk code uses these too
    private static final float PLANET_2_ORBIT = 3600f;
    private static final float PLANET_2_RADIUS = 150f;

    // One dust-disk band's look: vanilla texture, which of its 4 bands, orbit days, spiral factor
    private static class DiskBand {
        final String texture;
        final int index;
        final float orbitDays;
        final float spiralFactor;

        DiskBand(String texture, int index, float orbitDays, float spiralFactor) {
            this.texture = texture;
            this.index = index;
            this.orbitDays = orbitDays;
            this.spiralFactor = spiralFactor;
        }
    }

    // Dust-disk bands in order from the innermost (center 1800) outward: 17 inner-section bands, then 35 outer-section
    // bands. These were the random picks in the "testing5_rings" save, fixed so every save gets the same disk.
    // The comment on each row is the band's center radius.
    private static final DiskBand[] DISK_BANDS = {
            new DiskBand("rings_dust0", 1, 14.147017f, 2.3367712f), // 1800
            new DiskBand("rings_dust0", 0, 16.359364f, 6.2806873f), // 1888.9
            new DiskBand("rings_dust0", 0, 18.868378f, 5.065893f), // 1977.8
            new DiskBand("rings_ice0", 1, 20.325537f, 6.9969683f), // 2066.6
            new DiskBand("rings_ice0", 3, 16.204874f, 2.2025495f), // 2155.5
            new DiskBand("rings_ice0", 2, 20.6631f, 3.357532f), // 2244.4
            new DiskBand("rings_dust0", 2, 17.506693f, 6.8296986f), // 2333.2
            new DiskBand("rings_dust0", 0, 23.107954f, 3.4467833f), // 2422.1
            new DiskBand("rings_ice0", 0, 21.094616f, 6.887002f), // 2511
            new DiskBand("rings_ice0", 2, 22.727125f, 3.9142697f), // 2599.9
            new DiskBand("rings_dust0", 1, 21.163034f, 3.122201f), // 2688.8
            new DiskBand("rings_dust0", 0, 24.858278f, 6.0976534f), // 2777.6
            new DiskBand("rings_dust0", 1, 28.522038f, 6.559695f), // 2866.5
            new DiskBand("rings_ice0", 2, 27.451939f, 5.9338884f), // 2955.4
            new DiskBand("rings_ice0", 2, 26.517141f, 3.3606124f), // 3044.2
            new DiskBand("rings_ice0", 3, 29.337555f, 4.4513607f), // 3133.1
            new DiskBand("rings_dust0", 1, 24.46814f, 2.3559077f), // 3222
            new DiskBand("rings_ice0", 3, 34.477444f, 5.3086023f), // 3978
            new DiskBand("rings_dust0", 1, 36.09115f, 3.7224727f), // 4066.9
            new DiskBand("rings_ice0", 2, 37.122223f, 3.7992842f), // 4155.8
            new DiskBand("rings_ice0", 1, 35.39882f, 6.242002f), // 4244.6
            new DiskBand("rings_dust0", 2, 33.738117f, 2.978544f), // 4333.5
            new DiskBand("rings_ice0", 3, 39.209694f, 5.333312f), // 4422.4
            new DiskBand("rings_ice0", 3, 36.17118f, 2.0823202f), // 4511.3
            new DiskBand("rings_ice0", 1, 39.956768f, 4.581041f), // 4600.2
            new DiskBand("rings_ice0", 2, 42.79655f, 5.209194f), // 4689.1
            new DiskBand("rings_ice0", 2, 43.534473f, 4.797146f), // 4777.9
            new DiskBand("rings_ice0", 0, 36.706383f, 4.3133616f), // 4866.8
            new DiskBand("rings_ice0", 2, 41.693993f, 6.746322f), // 4955.7
            new DiskBand("rings_ice0", 1, 45.865852f, 4.9177f), // 5044.6
            new DiskBand("rings_ice0", 3, 45.240837f, 6.542462f), // 5133.5
            new DiskBand("rings_ice0", 0, 43.312794f, 5.5974913f), // 5222.4
            new DiskBand("rings_ice0", 1, 40.402866f, 6.380484f), // 5311.2
            new DiskBand("rings_dust0", 1, 46.58232f, 3.208236f), // 5400.1
            new DiskBand("rings_dust0", 2, 52.587643f, 2.7063534f), // 5489
            new DiskBand("rings_dust0", 2, 42.734573f, 5.035054f), // 5577.9
            new DiskBand("rings_dust0", 2, 43.07314f, 4.147829f), // 5666.8
            new DiskBand("rings_dust0", 2, 47.67343f, 6.5265737f), // 5755.6
            new DiskBand("rings_dust0", 1, 45.20749f, 2.529455f), // 5844.5
            new DiskBand("rings_dust0", 0, 56.22343f, 4.042756f), // 5933.4
            new DiskBand("rings_dust0", 0, 48.857155f, 2.0775383f), // 6022.3
            new DiskBand("rings_ice0", 2, 48.11763f, 6.0939994f), // 6111.2
            new DiskBand("rings_ice0", 3, 55.94145f, 4.4843397f), // 6200.1
            new DiskBand("rings_ice0", 0, 49.147705f, 3.877945f), // 6288.9
            new DiskBand("rings_dust0", 3, 49.74319f, 5.845413f), // 6377.8
            new DiskBand("rings_dust0", 3, 51.84819f, 2.5307534f), // 6466.7
            new DiskBand("rings_dust0", 2, 55.248997f, 5.610029f), // 6555.6
            new DiskBand("rings_dust0", 3, 65.66013f, 3.4012468f), // 6644.5
            new DiskBand("rings_dust0", 0, 62.214245f, 2.5918193f), // 6733.4
            new DiskBand("rings_ice0", 0, 51.322525f, 4.8291373f), // 6822.2
            new DiskBand("rings_dust0", 2, 55.189495f, 3.7752793f), // 6911.1
            new DiskBand("rings_dust0", 2, 69.80369f, 4.3626833f), // 7000
    };

    public static boolean exists() {
        return Global.getSector().getStarSystem(SYSTEM_ID) != null;
    }

    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Leviathan");
        system.setOptionalUniqueId(SYSTEM_ID);
        // Same vanilla tags Knights of Ludd gives its abyss systems: keeps random missions, procgen specials and
        // pirate/Pather bases from picking Leviathan
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.THEME_UNSAFE);
        system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        system.setBackgroundTextureFilename("graphics/backgrounds/eis_leviathan_bg.png");
        system.setLightColor(LEVIATHAN_LIGHT);

        // Nebula: eis_leviathan_nebula.png is the layout (each non-black pixel is one 400-unit tile, centered on the star),
        // eis_nebula_pink is the cloud texture (the game uses eis_nebula_pink_map for the map view)
        Misc.addNebulaFromPNG("graphics/terrain/eis_leviathan_nebula.png",
                0f, 0f,
                system,
                "terrain", "eis_nebula_pink",
                4, 4, StarAge.AVERAGE);

        // Brown dwarf, radius 700 / corona 150
        PlanetAPI leviathan = system.initStar(STAR_ID, StarTypes.BROWN_DWARF, 700f, 22500f, 17500f, 150f);
        leviathan.setName("Leviathan");
        PlanetSpecAPI starSpec = leviathan.getSpec();
        starSpec.setTexture(Global.getSettings().getSpriteName("planets", "eis_pinkstar"));
        starSpec.setPlanetColor(new Color(255, 255, 255, 255));
        starSpec.setAtmosphereColor(new Color(237, 174, 192, 200));
        starSpec.setCoronaColor(new Color(237, 174, 192, 100));
        starSpec.setIconColor(new Color(237, 174, 192, 255));
        leviathan.applySpecChanges();

        // Dust disk, built like Bultach's Hubris accretion disk: overlapping spiral bands of vanilla rings_dust0 / rings_ice0
        // (texture, band, speed and spiral fixed per band in DISK_BANDS), band centers ~0.35 band widths apart (~90), so each
        // spot is covered by ~3 bands. Band centers run from 7000 in to 1800.
        // Tint and opacity ramp from untinted/fainter at the outer edge to Leviathan pink/opaque at the inner edge.
        // Split into an inner and an outer section around planet 2's orbit, each spaced to fit its own limits so the
        // visible edges on both sides of the gap sit exactly 100 units from planet 2's surface.
        float ringOuter = 7000f;
        float ringInner = 1800f;
        float ringWidth = 256f;
        float targetStep = 90f;
        float gapOuter = PLANET_2_ORBIT + PLANET_2_RADIUS + 100f; // 3850
        float innerSectionOuter = PLANET_2_ORBIT - PLANET_2_RADIUS - 100f; // 3350
        // Inner section: band centers from 1800 out to where the last band's outer edge meets innerSectionOuter
        float innerLastCenter = innerSectionOuter - ringWidth / 2f; // 3222
        int innerCount = Math.round((innerLastCenter - ringInner) / targetStep) + 1;
        // Outer section: band centers from 7000 in to where the first band's inner edge meets gapOuter
        float outerFirstCenter = gapOuter + ringWidth / 2f; // 3978
        int outerCount = Math.round((ringOuter - outerFirstCenter) / targetStep) + 1;
        float[] ringRadii = new float[innerCount + outerCount];
        for (int i = 0; i < innerCount; i++) {
            ringRadii[i] = ringInner + i * (innerLastCenter - ringInner) / (innerCount - 1);
        }
        for (int i = 0; i < outerCount; i++) {
            ringRadii[innerCount + i] = outerFirstCenter + i * (ringOuter - outerFirstCenter) / (outerCount - 1);
        }
        // Spirals curl inward from each band's inner edge down to minSpiralRadius, and a longer spiral spreads the band thinner.
        // Outer-section bands stop at the gap's outer edge, so the ones next to planet 2 stay short and dense.
        // Inner-section bands mirror that: a band's spiral is as long as its distance from the gap, so the ones next to
        // planet 2 stay short and dense too, never going below the disk's inner edge.
        float innerSpiralStop = ringInner - ringWidth / 2f;
        float outerSpiralStop = gapOuter;
        for (int i = 0; i < ringRadii.length; i++) {
            float ringRadius = ringRadii[i];
            boolean inInnerSection = i < innerCount;
            float t = (ringOuter - ringRadius) / (ringOuter - ringInner); // 0 = outermost band, 1 = innermost band
            Color tint = Misc.interpolateColor(Color.white, LEVIATHAN_PINK, t);
            tint = Misc.setAlpha(tint, (int) (150 + 105 * t));
            // Fixed texture/band/speed/spiral per band (see DISK_BANDS). The modulo only matters if the band count changes.
            DiskBand style = DISK_BANDS[i % DISK_BANDS.length];
            RingBandAPI band = system.addRingBand(leviathan, "misc", style.texture, 256f, style.index, tint, ringWidth, ringRadius, style.orbitDays);
            band.setSpiral(true);
            if (inInnerSection) {
                float distanceFromGap = innerSectionOuter - (ringRadius + ringWidth / 2f);
                band.setMinSpiralRadius(Math.max(innerSpiralStop, ringRadius - ringWidth / 2f - distanceFromGap));
            } else {
                band.setMinSpiralRadius(outerSpiralStop);
            }
            band.setSpiralFactor(style.spiralFactor);
        }

        // The spiral bands above are visuals only. These vanilla ring terrains (like Elysian Abyss's "Call of the Void" rings)
        // draw the disk on the map and give the vanilla "Hiding inside ring system" effect to slow-moving fleets.
        // Contiguous rings covering the disk from the innermost band's inner edge to the outermost band's outer edge:
        // skipping planet 2's gap: 2 rings for the inner section, 4 for the outer section.
        addRingTerrains(system, leviathan, ringInner - ringWidth / 2f, innerSectionOuter, 2);
        addRingTerrains(system, leviathan, gapOuter, ringOuter + ringWidth / 2f, 4);

        // Planets
        system.addPlanet("eis_leviathan_1", leviathan, "Bryyo", "cryovolcanic", 45f, 100f, 1500f, 60f);
        PlanetAPI norion = system.addPlanet("eis_leviathan_2", leviathan, "Norion", "tundra", 160f, PLANET_2_RADIUS, PLANET_2_ORBIT, 180f);
        addDamagedNexus(system, norion);
        // Roaming Remnant fleets, with vanilla's settings for a suppressed Remnant system: 7-12 fleets of 4-12 points
        // (x8 fleet points each), each with a 25% chance to be active
        system.addScript(new RemnantSeededFleetManager(system, 7, 12, 4, 12, 0.25f));
        PlanetAPI waterWorld = system.addPlanet("eis_leviathan_3", leviathan, "Aether", "water", 280f, 220f, 7500f, 375f);
        waterWorld.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "eis_pinkplanet2"));
        waterWorld.applySpecChanges();
        // Untinted eis_rings_pink ring (full 516px texture width) around the water world, drawn 256 wide like the ring around
        // Surtr in DA Mistral's Angelos; inner edge touches the planet's surface (220 + 256/2)
        system.addRingBand(waterWorld, "misc", "eis_rings_pink", 516f, 0, Color.white, 256f, 348f, 40f);
        SectorEntityToken waterRingTerrain = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(256f, 348f, waterWorld, null));
        waterRingTerrain.setCircularOrbit(waterWorld, 0, 0, 100);
        // Vanilla inactive gate orbiting the water world, as DA Mistral adds its Angelos Gate
        SectorEntityToken gate = system.addCustomEntity("eis_leviathan_gate", "Leviathan Gate", Entities.INACTIVE_GATE, Factions.NEUTRAL);
        gate.setCircularOrbit(waterWorld, 0f, 800f, 60f);
        PlanetAPI gasGiant = system.addPlanet("eis_leviathan_4", leviathan, "Serris", "gas_giant", 100f, 450f, 9000f, 450f);
        gasGiant.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "eis_leviathan_gas"));
        // Cloud layer: a copy of vanilla's gas giant clouds (clouds_banded01) to customise, tinted Leviathan pink
        // (vanilla's gas giant cloud alpha of 200 kept)
        gasGiant.getSpec().setCloudTexture(Global.getSettings().getSpriteName("planets", "eis_clouds_banded01"));
        gasGiant.getSpec().setCloudColor(new Color(237, 174, 192, 200));
        // Atmosphere glow in the same pink (vanilla's gas giant atmosphere alpha of 140 kept)
        gasGiant.getSpec().setAtmosphereColor(new Color(237, 174, 192, 140));
        gasGiant.applySpecChanges();

        // Gas giant moons
        system.addPlanet("eis_leviathan_5", gasGiant, "Rundas", "frozen", 0f, 70f, 900f, 20f);
        system.addPlanet("eis_leviathan_6", gasGiant, "Tallon", "rocky_ice", 180f, 60f, 1250f, 35f);

        // Asteroid belts around the gas giant: one between its moons, one outside them. 40 asteroids, 60 wide, like the belt
        // around Ciella in DA Mistral's Angelos (800 orbit, 55-75 days); orbit days scaled up by distance to match.
        // Each belt also gets a vanilla rings_asteroids0 band drawn on it, 256 wide like Ciella's, orbiting at the middle of
        // the belt's speed range: band 1 (second) for the inner belt, band 3 (fourth) for the outer belt.
        system.addAsteroidBelt(gasGiant, 40, 1075f, 60f, 72f, 98f);
        system.addRingBand(gasGiant, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 1075f, 85f);
        // Salvageable Remnant mining station in the inner belt, moving with the belt's ring band (85 days)
        SectorEntityToken miningStation = BaseThemeGenerator.addSalvageEntity(system, Entities.STATION_MINING_REMNANT, Factions.NEUTRAL);
        miningStation.setCircularOrbitPointingDown(gasGiant, 90f, 1075f, 85f);
        system.addAsteroidBelt(gasGiant, 40, 1475f, 60f, 96f, 131f);
        system.addRingBand(gasGiant, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 1475f, 114f);

        // Unowned objectives on the three stable points (an objective takes a stable location's place, as in vanilla
        // systems): comm relay out past Serris, nav buoy mid, sensor array outer. Named like vanilla's "Naraka Relay".
        SectorEntityToken commRelay = system.addCustomEntity("eis_leviathan_relay", "Leviathan Relay", Entities.COMM_RELAY, Factions.NEUTRAL);
        commRelay.setCircularOrbitPointingDown(leviathan, 200f, 11000f, 550f);
        SectorEntityToken navBuoy = system.addCustomEntity("eis_leviathan_nav_buoy", "Leviathan Nav Buoy", Entities.NAV_BUOY, Factions.NEUTRAL);
        navBuoy.setCircularOrbitPointingDown(leviathan, 60f, 4500f, 225f);
        SectorEntityToken sensorArray = system.addCustomEntity("eis_leviathan_sensor_array", "Leviathan Sensor Array", Entities.SENSOR_ARRAY, Factions.NEUTRAL);
        sensorArray.setCircularOrbitPointingDown(leviathan, 190f, 6300f, 450f);

        // Jump points, added before autogenerateHyperspaceJumpPoints so it creates their hyperspace ends
        JumpPointAPI innerJump = Global.getFactory().createJumpPoint("eis_leviathan_inner_jump", "Leviathan Inner Jump-point");
        innerJump.setCircularOrbit(leviathan, 0f, 2500f, 125f);
        innerJump.setStandardWormholeToHyperspaceVisual();
        system.addEntity(innerJump);
        JumpPointAPI outerJump = Global.getFactory().createJumpPoint("eis_leviathan_outer_jump", "Leviathan Outer Jump-point");
        outerJump.setCircularOrbit(leviathan, 150f, 7000f, 375f);
        outerJump.setStandardWormholeToHyperspaceVisual();
        system.addEntity(outerJump);

        system.autogenerateHyperspaceJumpPoints(true, false);
        // Added after the call above generates planet conditions: generation skips any planet that already has conditions
        waterWorld.getMarket().addCondition("eis_volatile_atmosphere");
        // Aether always has plentiful volatiles; drop any other volatiles level the random generation rolled
        waterWorld.getMarket().removeCondition(Conditions.VOLATILES_TRACE);
        waterWorld.getMarket().removeCondition(Conditions.VOLATILES_DIFFUSE);
        waterWorld.getMarket().removeCondition(Conditions.VOLATILES_ABUNDANT);
        if (!waterWorld.getMarket().hasCondition(Conditions.VOLATILES_PLENTIFUL)) {
            waterWorld.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);
        }
        clearHyperspaceNebula(system);
    }

    // Damaged Remnant Nexus orbiting the given planet, built the way vanilla's RemnantThemeGenerator.addBattlestations()
    // builds one for a suppressed Remnant system: station-mode fleet, Alpha core commander, transponder on, slow spin.
    private void addDamagedNexus(StarSystemAPI system, PlanetAPI focus) {
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, FleetTypes.BATTLESTATION, null);
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "remnant_station2_Damaged");
        fleet.getFleetData().addFleetMember(member);

        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleet.getMemoryWithoutUpdate().set("$damagedStation", true);
        fleet.addTag(Tags.NEUTRINO_HIGH);
        fleet.setStationMode(true);
        RemnantThemeGenerator.addRemnantStationInteractionConfig(fleet);

        system.addEntity(fleet);
        fleet.clearAbilities();
        fleet.addAbility(Abilities.TRANSPONDER);
        fleet.getAbility(Abilities.TRANSPONDER).activate();
        fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);
        fleet.setAI(null);
        fleet.setName(fleet.getName() + " (Damaged)");

        // 150 beyond Norion's surface (300 from its center), spinning at vanilla's 5 degrees/second
        fleet.setCircularOrbitWithSpin(focus, 0f, focus.getRadius() + 150f, 30f, 5f, 5f);

        // Alpha core commander, as vanilla uses for damaged stations too (no extra skills: damaged is already weaker)
        PersonAPI commander = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE)
                .createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), new Random());
        fleet.setCommander(commander);
        fleet.getFlagship().setCaptain(commander);
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        // Spawns Remnant fleets from the station, with vanilla's settings for a damaged station in a suppressed system:
        // 2-4 fleets at most, active when the player is within 1 light-year, 25 days to respawn, 6-12 fleet points
        int maxFleets = 2 + new Random().nextInt(3);
        system.addScript(new RemnantStationFleetManager(fleet, 1f, 0, maxFleets, 25f, 6, 12));
    }

    // Fills the span from inner to outer with the given number of equal-width, edge-to-edge vanilla ring terrains.
    private void addRingTerrains(StarSystemAPI system, SectorEntityToken focus, float inner, float outer, int count) {
        float width = (outer - inner) / count;
        for (int i = 0; i < count; i++) {
            float middle = inner + width * (i + 0.5f);
            SectorEntityToken ringTerrain = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(width, middle, focus, null));
            ringTerrain.setCircularOrbit(focus, 0, 0, 100);
        }
    }

    // Clears hyperspace storms around the system's hyperspace location, as vanilla does for its own systems.
    private void clearHyperspaceNebula(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }
}
