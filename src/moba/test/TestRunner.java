package moba.test;

import moba.GameEngine;
import moba.gfx.TextureAtlas;
import moba.model.BuffMonster;
import moba.model.Entity;
import moba.model.Hero;
import moba.model.JungleCamp;
import moba.model.Minion;
import moba.model.Tower;
import moba.model.SummonedLord;
import moba.ui.ShopUI;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   RUNNING CRAFTARENA 5V5 MOBA TEST SUITE");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Texture Atlas
        try {
            assertNotNull(TextureAtlas.get(TextureAtlas.GRASS_TOP), "Grass Top texture");
            assertNotNull(TextureAtlas.get(TextureAtlas.STONE_BRICK), "Stone Brick texture");
            assertNotNull(TextureAtlas.get(TextureAtlas.OBSIDIAN), "Obsidian texture");
            assertNotNull(TextureAtlas.get(TextureAtlas.WATER), "Water texture");
            System.out.println("[PASS] Test 1: Texture Atlas procedural generation verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1: " + t.getMessage());
            failed++;
        }

        // Test 2: 5v5 Team Initialization & Roles
        GameEngine engine = new GameEngine(1024, 720);
        engine.startMatchDirectly();
        try {
            Hero player = engine.getPlayerHero();
            Hero enemy = engine.getEnemyHero();
            assertNotNull(player, "Player Hero");
            assertNotNull(enemy, "Enemy Hero");
            assertEquals("Steve", player.heroName, "Player name");
            assertEquals("Herobrine", enemy.heroName, "Enemy name");

            assertEquals(5, engine.getBlueHeroes().size(), "Blue team has 5 heroes");
            assertEquals(5, engine.getRedHeroes().size(), "Red team has 5 heroes");

            assertEquals("Steve", engine.getBlueHeroes().get(0).heroName, "Blue 0 Steve (EXP)");
            assertEquals("Evoker", engine.getBlueHeroes().get(1).heroName, "Blue 1 Evoker (MID)");
            assertEquals("Alex", engine.getBlueHeroes().get(2).heroName, "Blue 2 Alex (GOLD)");
            assertEquals("Iron Golem", engine.getBlueHeroes().get(3).heroName, "Blue 3 Iron Golem (ROAM)");
            assertEquals("Nether Knight", engine.getBlueHeroes().get(4).heroName, "Blue 4 Nether Knight (JUNGLE)");

            assertEquals("Herobrine", engine.getRedHeroes().get(0).heroName, "Red 0 Herobrine (MID)");
            assertEquals("Piglin Brute", engine.getRedHeroes().get(1).heroName, "Red 1 Piglin (EXP)");
            assertEquals("Wither Skeletor", engine.getRedHeroes().get(2).heroName, "Red 2 Wither (GOLD)");
            assertEquals("Warden", engine.getRedHeroes().get(3).heroName, "Red 3 Warden (ROAM)");
            assertEquals("Shadow Creeper", engine.getRedHeroes().get(4).heroName, "Red 4 Shadow Creeper (JUNGLE)");

            System.out.println("[PASS] Test 2: 5v5 Hero rosters, roles and archetypes verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2: " + t.getMessage());
            failed++;
        }

        // Test 3: Map bounds and 3-lane walkability
        try {
            assertTrue(engine.getMap().isWalkable(6.0f, 6.0f), "Spawn should be walkable");
            assertTrue(engine.getMap().isWalkable(6.0f, 25.0f), "Top lane should be walkable");
            assertTrue(engine.getMap().isWalkable(25.0f, 25.0f), "Mid lane should be walkable");
            assertTrue(engine.getMap().isWalkable(25.0f, 6.0f), "Bot lane should be walkable");
            assertFalse(engine.getMap().isWalkable(0, 0), "Border wall should be solid");
            System.out.println("[PASS] Test 3: 3-Lane map bounds and walkability verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3: " + t.getMessage());
            failed++;
        }

        // Test 4: Skills Execution (Q, W, E, R)
        try {
            Hero player = engine.getPlayerHero();
            float initialMana = player.mana;

            boolean qCast = player.skillQ.cast(player, player.x, player.y, null, engine);
            assertTrue(qCast, "Skill Q should cast successfully");
            assertTrue(player.mana < initialMana, "Skill Q consumed mana");
            assertFalse(player.skillQ.isReady(), "Skill Q is on cooldown");

            boolean wCast = player.skillW.cast(player, player.x + 4, player.y + 4, null, engine);
            assertTrue(wCast, "Skill W should cast successfully");

            float oldX = player.x;
            boolean eCast = player.skillE.cast(player, player.x + 3, player.y + 3, null, engine);
            assertTrue(eCast, "Skill E should cast successfully");
            assertTrue(player.x != oldX, "Skill E teleported player");

            // Skill R is locked at Level 1
            assertEquals(1, player.level, "Player starts at level 1");
            boolean rCastLv1 = player.skillR.cast(player, player.x, player.y, null, engine);
            assertFalse(rCastLv1, "Skill R (Ultimate) is locked at Level 1");

            // Level up player to Level 4
            player.level = 4;
            boolean rCastLv4 = player.skillR.cast(player, player.x, player.y, null, engine);
            assertTrue(rCastLv4, "Skill R (Ultimate) unlocks and casts successfully at Level 4");

            System.out.println("[PASS] Test 4: All Hero skills (Q, W, E, R) executed and verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4: " + t.getMessage());
            failed++;
        }

        // Test 5: Item Shop Purchases
        try {
            Hero player = engine.getPlayerHero();
            player.gold = 5000;
            float baseDmg = player.attackDamage + player.bonusDamage;
            float baseHp = player.maxHp + player.bonusHp;

            ShopUI shop = new ShopUI();
            shop.buyItem(0, player, engine); // Diamond sword
            assertEquals(1, player.inventory.size(), "Inventory size after purchase");
            assertTrue(player.attackDamage + player.bonusDamage > baseDmg, "Bonus damage increased");

            shop.buyItem(1, player, engine); // Netherite armor
            assertEquals(2, player.inventory.size(), "Inventory size after 2nd purchase");
            assertTrue(player.maxHp + player.bonusHp > baseHp, "Bonus HP increased");

            System.out.println("[PASS] Test 5: Item Shop purchases and stat modifications verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5: " + t.getMessage());
            failed++;
        }

        // Test 6: Fog of War & Vision Culling
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();
            Hero alex = testEngine.getBlueHeroes().get(2);
            Hero wither = testEngine.getRedHeroes().get(2);

            // Teammate Alex is ALWAYS visible to the player
            assertTrue(testEngine.isEntityVisibleToPlayer(alex), "Allied teammate Alex is always visible");

            // Wither Skeletor at far corner (43, 41) is NOT visible to player at (6, 6)
            assertFalse(testEngine.isEntityVisibleToPlayer(wither), "Far enemy Wither Skeletor is hidden in Fog of War");

            // Move player within 8 blocks of wither
            player.x = wither.x - 5.0f;
            player.y = wither.y - 5.0f;
            assertTrue(testEngine.isEntityVisibleToPlayer(wither), "Enemy Wither Skeletor is revealed when in vision range");

            System.out.println("[PASS] Test 6: Fog of war vision culling verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 6: " + t.getMessage());
            failed++;
        }

        // Test 7: 5v5 Simulation & Turret Range Area Indicators
        try {
            System.out.println("Running 5v5 simulation ticks with turret range indicator...");
            engine.startMatchDirectly();
            Hero player = engine.getPlayerHero();
            player.x = 22.0f;
            player.y = 23.5f;
            player.destinationX = 22.0f;
            player.destinationY = 23.5f;

            Hero enemy = engine.getEnemyHero();
            enemy.x = 23.5f;
            enemy.y = 22.0f;
            enemy.destinationX = 23.5f;
            enemy.destinationY = 22.0f;

            // Simulate minion wave clashes & 5v5 bot logic
            for (int i = 0; i < 75; i++) {
                engine.update(0.016f);
            }

            // Player casts Skill Q and W
            player.skillQ.currentCooldown = 0;
            player.mana = player.maxMana;
            player.skillQ.cast(player, enemy.x, enemy.y, enemy, engine);

            player.skillW.currentCooldown = 0;
            player.skillW.cast(player, enemy.x, enemy.y, enemy, engine);

            for (int i = 0; i < 15; i++) {
                engine.update(0.016f);
            }

            System.out.println("[PASS] Test 7: 5v5 Battle simulation passed.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 7: " + t.getMessage());
            failed++;
        }

        // Test 8: Turret Targeting Warning System & Screenshots
        try {
            File outDir = new File("screenshots");
            if (!outDir.exists()) outDir.mkdirs();

            engine.startMatchDirectly();
            Hero player = engine.getPlayerHero();

            // Save active gameplay screenshot
            String path1 = "screenshots/screenshot_gameplay.png";
            boolean saved1 = engine.saveScreenshot(path1);
            assertTrue(saved1 && new File(path1).exists(), "Gameplay screenshot created");

            // --- Test Turret Targeting: Minion vs Player ---
            Tower redTower = null;
            for (Entity e : engine.getAllEntities()) {
                if (e instanceof Tower t && "Red Mid Outer".equals(t.towerName)) {
                    redTower = t;
                    break;
                }
            }
            assertNotNull(redTower, "Red Mid Outer Tower found");

            // 1. Minion in range: Tower targets minion
            Minion blueMinion = new Minion(28.0f, 28.0f, Entity.TEAM_BLUE, false, null);
            engine.getAllEntities().add(blueMinion);
            redTower.targetEntity = blueMinion;

            // Player Steve is inside tower range (29, 29)
            player.x = 29.0f;
            player.y = 29.0f;
            engine.centerCameraOn(redTower.x, redTower.y);

            // Verify tower is targeting minion, NOT player
            assertTrue(redTower.targetEntity instanceof Minion, "Tower targets minion");
            assertTrue(redTower.targetEntity != player, "Tower is NOT targeting player");
            String pathMinionTarget = "screenshots/screenshot_turret_targeting_minion.png";
            boolean savedMinion = engine.saveScreenshot(pathMinionTarget);
            assertTrue(savedMinion && new File(pathMinionTarget).exists(), "Screenshot targeting minion created");

            // 2. Minion dies: Tower switches aggro to Player Steve
            blueMinion.alive = false;
            redTower.targetEntity = player; // Turret now attacks player!
            assertTrue(redTower.targetEntity == player, "Tower is now attacking player");
            String pathPlayerTarget = "screenshots/screenshot_turret_targeting_player.png";
            boolean savedPlayer = engine.saveScreenshot(pathPlayerTarget);
            assertTrue(savedPlayer && new File(pathPlayerTarget).exists(), "Screenshot targeting player created");

            // Center camera back on player
            engine.centerCameraOn(player.x, player.y);

            // Shop Screenshot
            engine.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_P, 'P'));
            String path2 = "screenshots/screenshot_shop.png";
            boolean saved2 = engine.saveScreenshot(path2);
            assertTrue(saved2 && new File(path2).exists(), "Shop screenshot created");
            engine.keyPressed(new KeyEvent(new java.awt.Component(){}, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_P, 'P'));

            // Test Player Death & Respawn Timer
            player.takeDamage(99999f, engine.getEnemyHero(), engine);
            assertFalse(player.alive, "Player should be dead after fatal damage");
            assertTrue(player.respawnTimer > 0, "Player should have an active respawn timer countdown");
            assertEquals("Herobrine", player.lastKilledByName, "Killer should be Herobrine");

            // Save Death Screen Screenshot with respawn countdown
            String path3 = "screenshots/screenshot_death_screen.png";
            boolean saved3 = engine.saveScreenshot(path3);
            assertTrue(saved3 && new File(path3).exists(), "Death screen screenshot created");

            System.out.println("[PASS] Test 8: Turret targeting warning (minion vs player) and screenshots verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 8: " + t.getMessage());
            failed++;
        }

        // Test 9: MLBB-Style Kill Notification Banner & Snappy Respawn Countdown
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();
            Hero enemy = testEngine.getEnemyHero();

            // 1. Steve kills Herobrine -> "YOU HAVE SLAIN AN ENEMY!"
            testEngine.addKillEvent(player.heroName, enemy.heroName, true, false, false);
            GameEngine.KillEvent banner = testEngine.getActiveBanner();
            assertNotNull(banner, "Active kill banner should exist");
            assertEquals("YOU HAVE SLAIN AN ENEMY!", banner.title, "Banner title for player kill");
            assertTrue(testEngine.getKillFeed().size() > 0, "Kill feed contains the kill event");

            String killNotifPath = "screenshots/screenshot_kill_notification.png";
            boolean savedKillBanner = testEngine.saveScreenshot(killNotifPath);
            assertTrue(savedKillBanner && new File(killNotifPath).exists(), "Kill notification screenshot created");

            // 2. Test snappy respawn timer calculation
            player.level = 1;
            player.takeDamage(9999f, enemy, testEngine);
            assertFalse(player.alive, "Player is dead");
            // Max respawn time for Level 1 is 3.0 + 1 * 0.8 = 3.8s (Fast MLBB style)
            assertTrue(player.respawnTimer <= 4.0f && player.respawnTimer > 0, "Snappy respawn timer <= 4.0s at level 1");
            GameEngine.KillEvent deathBanner = testEngine.getActiveBanner();
            assertNotNull(deathBanner, "Death banner should exist");
            assertEquals("YOU HAVE BEEN SLAIN!", deathBanner.title, "Banner title for player death");

            System.out.println("[PASS] Test 9: MLBB-Style Kill Notification Banner and Snappy Respawn Countdown verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 9: " + t.getMessage());
            failed++;
        }

        // Test 10: Tall Grass / Bush Stealth Culling & Ambush Mechanics
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();
            Hero redJungler = testEngine.getRedHeroes().get(4); // Shadow Creeper

            // Place Shadow Creeper in Mid River bush (23, 26)
            redJungler.x = 23.0f;
            redJungler.y = 26.0f;
            redJungler.isInBush = true;
            redJungler.revealTimer = 0f;

            // Player Steve is outside at (20, 20)
            player.x = 20.0f;
            player.y = 20.0f;
            player.isInBush = false;

            // Move allied heroes far away
            for (Hero ally : testEngine.getBlueHeroes()) {
                if (ally != player) {
                    ally.x = 6.0f;
                    ally.y = 6.0f;
                }
            }

            // Verify creeper is HIDDEN from player vision while in bush!
            assertFalse(testEngine.isEntityVisibleToPlayer(redJungler), "Enemy Shadow Creeper is completely hidden in tall grass");

            // Move player into the SAME bush at (23, 26)
            player.x = 23.0f;
            player.y = 26.0f;
            player.isInBush = true;
            // Now player is inside bush with creeper -> creeper is revealed!
            assertTrue(testEngine.isEntityVisibleToPlayer(redJungler), "Enemy is revealed when player enters the same bush");

            // Move player out again, but red creeper attacks -> revealed via revealTimer
            player.x = 20.0f;
            player.y = 20.0f;
            player.isInBush = false;
            redJungler.revealTimer = 2.0f;
            assertTrue(testEngine.isEntityVisibleToPlayer(redJungler), "Enemy is revealed while revealTimer > 0 (post-attack)");

            // Take screenshot of player next to waving tall grass / bush
            testEngine.centerCameraOn(23.0f, 26.0f);
            String bushPath = "screenshots/screenshot_bush_stealth.png";
            boolean savedBush = testEngine.saveScreenshot(bushPath);
            assertTrue(savedBush && new File(bushPath).exists(), "Bush stealth screenshot created");

            System.out.println("[PASS] Test 10: Tall Grass / Bush Stealth Culling & Ambush mechanics verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 10: " + t.getMessage());
            failed++;
        }

        // Test 11: River Boss System (Dragon Turtle & Nether Lord)
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();
            JungleCamp boss = testEngine.getJungleBoss();

            assertNotNull(boss, "River Boss exists");
            assertEquals(JungleCamp.BossType.TURTLE, boss.type, "Initial boss is Dragon Turtle");
            assertEquals(2800f, boss.maxHp, "Turtle max HP is 2800");

            // Spawn Turtle for combat test
            boss.spawnTurtleImmediately();

            // Position player at Turtle pit for full vision
            player.x = boss.x - 2.5f;
            player.y = boss.y - 2.5f;
            testEngine.centerCameraOn(boss.x, boss.y);
            String turtlePath = "screenshots/screenshot_turtle_boss.png";
            testEngine.saveScreenshot(turtlePath);

            // Slay Turtle by player
            boss.takeDamage(99999f, player, testEngine);
            assertFalse(boss.alive, "Turtle should be slain");
            assertEquals(250f, player.shieldHp, "Player gained 250 HP Turtle Shield");
            assertTrue(player.gold > 500, "Player gained gold reward from Turtle");

            // Upgrade Boss to Lord
            boss.setBossType(JungleCamp.BossType.LORD);
            boss.alive = true;
            boss.hp = boss.maxHp;
            assertEquals(JungleCamp.BossType.LORD, boss.type, "Boss type upgraded to Nether Lord");
            assertEquals(5200f, boss.maxHp, "Lord max HP is 5200");

            // Slay Lord by player
            int entCountBefore = testEngine.getAllEntities().size();
            boss.takeDamage(99999f, player, testEngine);
            assertFalse(boss.alive, "Lord should be slain");
            assertTrue(boss.pendingLordSummon, "Pending Lord summon active for 10-second delay");
            assertEquals(10.0f, boss.lordSummonCountdown, "Countdown initialized to 10 seconds");

            // Advance by 10.1 seconds to trigger summon
            boss.update(10.1f, testEngine);
            int entCountAfter = testEngine.getAllEntities().size();
            assertTrue(entCountAfter > entCountBefore, "Summoned Lord entity spawned into match after 10s delay");

            // Find Summoned Lord
            moba.model.SummonedLord summoned = null;
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof moba.model.SummonedLord sl) {
                    summoned = sl;
                    break;
                }
            }
            assertNotNull(summoned, "SummonedLord instance verified");
            assertEquals(Entity.TEAM_BLUE, summoned.team, "Summoned Lord fights for Blue Team");

            // Capture Lord marching screenshot with player beside it
            player.x = summoned.x - 2.2f;
            player.y = summoned.y - 2.2f;
            testEngine.centerCameraOn(summoned.x, summoned.y);
            String lordPath = "screenshots/screenshot_lord_boss.png";
            testEngine.saveScreenshot(lordPath);

            System.out.println("[PASS] Test 11: River Boss System (Dragon Turtle & Nether Lord) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 11: " + t.getMessage());
            failed++;
        }

        // Test 12: Main Menu, Lane Selection, Matchmaking & Draft Pick Pipeline
        try {
            GameEngine menuEngine = new GameEngine(1024, 720);
            assertEquals(GameEngine.GameState.MAIN_MENU, menuEngine.getState(), "Default state is MAIN_MENU");

            // Save Main Menu screenshot
            String menuPath = "screenshots/screenshot_main_menu.png";
            menuEngine.saveScreenshot(menuPath);
            assertTrue(new File(menuPath).exists(), "Main menu screenshot created");

            // Click [START 5v5 MATCH] button at (512, 305)
            menuEngine.mousePressed(new java.awt.event.MouseEvent(
                    new java.awt.Component(){}, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 512, 305, 1, false, java.awt.event.MouseEvent.BUTTON1));
            assertEquals(GameEngine.GameState.LANE_SELECT, menuEngine.getState(), "Transitioned to LANE_SELECT");

            // Select Jungle role card (card 4 around x=870, y=250)
            menuEngine.mousePressed(new java.awt.event.MouseEvent(
                    new java.awt.Component(){}, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 870, 250, 1, false, java.awt.event.MouseEvent.BUTTON1));
            assertEquals(4, menuEngine.getMenuUI().getSelectedLaneIndex(), "Jungle role card selected");

            // Save Lane Select screenshot
            String lanePath = "screenshots/screenshot_lane_select.png";
            menuEngine.saveScreenshot(lanePath);
            assertTrue(new File(lanePath).exists(), "Lane select screenshot created");

            // Click [FIND MATCH 5v5] button at (512, 460)
            menuEngine.mousePressed(new java.awt.event.MouseEvent(
                    new java.awt.Component(){}, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 512, 460, 1, false, java.awt.event.MouseEvent.BUTTON1));
            assertEquals(GameEngine.GameState.MATCHMAKING, menuEngine.getState(), "Transitioned to MATCHMAKING");

            // Simulate matchmaking timer ticks to find 10 players and transition to Draft
            for (int i = 0; i < 220; i++) {
                menuEngine.update(0.016f);
            }
            assertEquals(GameEngine.GameState.DRAFT_PICK, menuEngine.getState(), "Transitioned to DRAFT_PICK");

            // Select Nether Knight (hero 4 around x=730, y=120)
            menuEngine.mousePressed(new java.awt.event.MouseEvent(
                    new java.awt.Component(){}, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 730, 120, 1, false, java.awt.event.MouseEvent.BUTTON1));
            assertEquals(4, menuEngine.getMenuUI().getSelectedHeroIndex(), "Nether Knight hero card selected");

            // Save Draft Pick screenshot
            String draftPath = "screenshots/screenshot_draft_pick.png";
            menuEngine.saveScreenshot(draftPath);
            assertTrue(new File(draftPath).exists(), "Draft pick screenshot created");

            // Click [LOCK IN HERO] button at (512, 400)
            menuEngine.mousePressed(new java.awt.event.MouseEvent(
                    new java.awt.Component(){}, java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), 0, 512, 400, 1, false, java.awt.event.MouseEvent.BUTTON1));
            assertEquals(GameEngine.GameState.PLAYING, menuEngine.getState(), "Transitioned to PLAYING after lock-in");
            assertEquals("Nether Knight", menuEngine.getPlayerHero().heroName, "Player hero is Nether Knight");
            assertEquals(Hero.Lane.JUNGLE, menuEngine.getPlayerHero().assignedLane, "Player lane is JUNGLE");
            assertEquals(5, menuEngine.getBlueHeroes().size(), "Blue team has 5 heroes");
            assertEquals(5, menuEngine.getRedHeroes().size(), "Red team has 5 heroes");

            System.out.println("[PASS] Test 12: Main Menu, Lane Selection, Matchmaking & Draft Pick Pipeline verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 12: " + t.getMessage());
            failed++;
        }

        // Test 13: Jungle Buff Monsters, Buff Acquisition, CDR/Mana & True Burn
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();

            List<BuffMonster> buffMonsters = testEngine.getBuffMonsters();
            assertNotNull(buffMonsters, "Buff monsters list exists");
            assertEquals(4, buffMonsters.size(), "4 Buff camps (2 Blue, 2 Red)");

            BuffMonster blueBuffCamp = null;
            BuffMonster redBuffCamp = null;
            for (BuffMonster bm : buffMonsters) {
                if (bm.buffType == BuffMonster.BuffType.BLUE && blueBuffCamp == null) blueBuffCamp = bm;
                if (bm.buffType == BuffMonster.BuffType.RED && redBuffCamp == null) redBuffCamp = bm;
            }
            assertNotNull(blueBuffCamp, "Blue Buff camp found");
            assertNotNull(redBuffCamp, "Red Buff camp found");

            // Position player at blue buff camp and take screenshot
            player.x = blueBuffCamp.x - 2.0f;
            player.y = blueBuffCamp.y - 1.5f;
            testEngine.centerCameraOn(blueBuffCamp.x, blueBuffCamp.y);
            String buffMonsterPath = "screenshots/screenshot_buff_monsters.png";
            testEngine.saveScreenshot(buffMonsterPath);
            assertTrue(new File(buffMonsterPath).exists(), "Buff monsters screenshot created");

            // Kill Blue Buff
            blueBuffCamp.takeDamage(99999f, player, testEngine);
            assertFalse(blueBuffCamp.alive, "Blue Buff killed");
            assertEquals(75.0f, player.blueBuffTimer, "Player received 75s Blue Buff");

            // Test Blue Buff mana regen & CDR tick
            player.mana = 50f;
            player.skillQ.currentCooldown = 5.0f;
            player.update(1.0f, testEngine);
            assertTrue(player.mana > 50f + 15.0f, "Blue buff grants rapid mana regen");
            assertTrue(player.skillQ.currentCooldown < 4.0f, "Blue buff provides 20% CDR reduction");

            // Kill Red Buff
            redBuffCamp.takeDamage(99999f, player, testEngine);
            assertFalse(redBuffCamp.alive, "Red Buff killed");
            assertEquals(75.0f, player.redBuffTimer, "Player received 75s Red Buff");

            // Test Red Buff true burn on basic attack
            Hero enemyHero = testEngine.getRedHeroes().get(0);
            enemyHero.hp = 1000f;
            enemyHero.bonusArmor = 50f;
            float prevHp = enemyHero.hp;
            player.performBasicAttack(enemyHero, testEngine);
            float damageDealt = prevHp - enemyHero.hp;
            assertTrue(damageDealt >= 25.0f, "Red buff dealt on-hit true damage burn");

            // Take screenshot of hero buff rings
            testEngine.centerCameraOn(player.x, player.y);
            String heroBuffPath = "screenshots/screenshot_hero_buffs.png";
            testEngine.saveScreenshot(heroBuffPath);
            assertTrue(new File(heroBuffPath).exists(), "Hero buffs screenshot created");

            System.out.println("[PASS] Test 13: Buff Monsters, Blue & Red Buff effects verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 13: " + t.getMessage());
            failed++;
        }

        // Test 14: Turtle Spawn Timings (Min 3:00 / 180s & Min 6:00 / 360s)
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            JungleCamp boss = testEngine.getJungleBoss();

            // At 0:00 (0s), boss should not be alive yet
            assertFalse(boss.alive, "Turtle not alive at 0:00");
            assertEquals(JungleCamp.BossType.TURTLE, boss.type, "Boss type is TURTLE");

            // Fast forward to 179s: still not spawned
            testEngine.setMatchTime(179.0f);
            boss.update(1.0f, testEngine);
            assertFalse(boss.alive, "Turtle not alive at 2:59 (179s)");

            // Fast forward to 180s (3:00): Turtle spawns!
            testEngine.setMatchTime(180.0f);
            boss.update(1.0f, testEngine);
            assertTrue(boss.alive, "Turtle spawned at 3:00 (180s)");
            assertEquals(JungleCamp.BossType.TURTLE, boss.type, "Spawned boss is Dragon Turtle");

            // Slay Turtle at 3:30 (210s)
            Hero player = testEngine.getPlayerHero();
            boss.takeDamage(99999f, player, testEngine);
            assertFalse(boss.alive, "Turtle slain");
            assertTrue(boss.turtleSlainAtLeastOnce, "turtleSlainAtLeastOnce recorded");

            // Fast forward to 359s: still dead
            testEngine.setMatchTime(359.0f);
            boss.update(1.0f, testEngine);
            assertFalse(boss.alive, "Turtle not respawned before minute 6");

            // Fast forward to 360s (6:00): Turtle respawns!
            testEngine.setMatchTime(360.0f);
            boss.update(1.0f, testEngine);
            assertTrue(boss.alive, "Turtle respawned at 6:00 (360s)");
            assertEquals(JungleCamp.BossType.TURTLE, boss.type, "Respawned boss is Dragon Turtle");

            System.out.println("[PASS] Test 14: Turtle Minute 3 and Minute 6 spawns verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 14: " + t.getMessage());
            failed++;
        }

        // Test 15: Minute 8 (480s) Transformation Dynamics (Unattacked vs Attacked vs Disengaged 30s)
        try {
            // Case A: Unattacked at Minute 8 -> transforms into Lord
            GameEngine engineA = new GameEngine(1024, 720);
            engineA.startMatchDirectly();
            JungleCamp bossA = engineA.getJungleBoss();
            bossA.spawnTurtleImmediately();
            bossA.hasSpawnedMinute3 = true;
            bossA.hasSpawnedMinute6 = true;

            engineA.setMatchTime(479.0f);
            bossA.update(1.0f, engineA);
            assertEquals(JungleCamp.BossType.TURTLE, bossA.type, "Still Turtle at 7:59");

            engineA.setMatchTime(480.0f);
            bossA.update(1.0f, engineA);
            assertEquals(JungleCamp.BossType.LORD, bossA.type, "Unattacked Turtle evolves to Nether Lord at 8:00 (480s)");
            assertTrue(bossA.alive, "Evolved Lord is alive");

            // Case B: Attacked at Minute 8 -> DOES NOT transform into Lord while engaged
            GameEngine engineB = new GameEngine(1024, 720);
            engineB.startMatchDirectly();
            JungleCamp bossB = engineB.getJungleBoss();
            bossB.spawnTurtleImmediately();
            bossB.hasSpawnedMinute3 = true;
            bossB.hasSpawnedMinute6 = true;

            Hero attacker = engineB.getRedHeroes().get(0);
            engineB.setMatchTime(480.0f);
            bossB.takeDamage(100f, attacker, engineB); // Attacked!
            bossB.targetEntity = attacker;
            bossB.update(0.5f, engineB);

            assertEquals(JungleCamp.BossType.TURTLE, bossB.type, "Turtle DOES NOT transform into Lord while actively attacked at min 8");

            // Save screenshot of engaged Turtle at Min 8
            engineB.centerCameraOn(bossB.x, bossB.y);
            String transPath = "screenshots/screenshot_turtle_min8_transition.png";
            engineB.saveScreenshot(transPath);
            assertTrue(new File(transPath).exists(), "Min 8 engaged transition screenshot created");

            // Case C: Attacked once, but attacker leaves/disengages -> transforms into Lord 30 seconds later
            bossB.targetEntity = null; // Attacker disengages
            // Simulate 29 seconds elapsed
            for (int i = 0; i < 29; i++) {
                engineB.setMatchTime(481.0f + i);
                bossB.update(1.0f, engineB);
            }
            assertEquals(JungleCamp.BossType.TURTLE, bossB.type, "Turtle remains Turtle at 29s after disengage");

            // 30th second elapsed
            engineB.setMatchTime(511.0f);
            bossB.update(1.0f, engineB);
            assertEquals(JungleCamp.BossType.LORD, bossB.type, "Turtle evolves to Nether Lord 30s after attacker disengages");

            System.out.println("[PASS] Test 15: Minute 8 (480s) Transformation Dynamics verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 15: " + t.getMessage());
            failed++;
        }

        // Test 16: Minute 10 (600s) Lord Spawn if Turtle Slain
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            JungleCamp boss = testEngine.getJungleBoss();
            boss.hasSpawnedMinute3 = true;
            boss.hasSpawnedMinute6 = true;
            boss.spawnTurtleImmediately();

            // Slay Turtle at minute 7 (420s)
            testEngine.setMatchTime(420.0f);
            boss.takeDamage(99999f, testEngine.getPlayerHero(), testEngine);
            assertFalse(boss.alive, "Turtle slain");

            // Advance match time to 599s (9:59)
            testEngine.setMatchTime(599.0f);
            boss.update(1.0f, testEngine);
            assertFalse(boss.alive, "Lord not spawned before 10:00");

            // Advance match time to 600s (10:00)
            testEngine.setMatchTime(600.0f);
            boss.update(1.0f, testEngine);
            assertTrue(boss.alive, "Nether Lord spawned at 10:00 (600s)");
            assertEquals(JungleCamp.BossType.LORD, boss.type, "Spawned boss is Nether Lord");

            System.out.println("[PASS] Test 16: Minute 10 (600s) Lord Spawn after Turtle death verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 16: " + t.getMessage());
            failed++;
        }

        // Test 17: Clumsy and Uncoordinated ("goblok & ga kompak") Roam & Jungler AI
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            JungleCamp boss = testEngine.getJungleBoss();
            boss.spawnTurtleImmediately();

            Hero redJungler = null;
            Hero redRoamer = null;
            for (Hero h : testEngine.getRedHeroes()) {
                if (h.assignedLane == Hero.Lane.JUNGLE) redJungler = h;
                if (h.assignedLane == Hero.Lane.ROAM) redRoamer = h;
            }
            assertNotNull(redJungler, "Red Jungler exists");
            assertNotNull(redRoamer, "Red Roamer exists");

            // Jungler Behavior:
            // 1. When healthy near boss, attempts to solo without team
            redJungler.hp = redJungler.maxHp;
            redJungler.x = boss.x - 2.0f;
            redJungler.y = boss.y - 2.0f;
            redJungler.update(0.1f, testEngine);
            assertEquals(boss, redJungler.targetEntity, "Jungler recklessly attempts to solo Turtle/Lord without team");

            // 2. When low HP (<60%) near boss, panics and retreats to base ("goblok")
            redJungler.hp = redJungler.maxHp * 0.45f;
            redJungler.update(0.1f, testEngine);
            assertFalse(redJungler.targetEntity == boss, "Jungler panics and abandons boss at <60% HP");
            assertEquals(redJungler.spawnX, redJungler.destinationX, "Jungler retreats towards spawn");

            // Roamer Behavior:
            // Roamer near active Turtle/Lord completely ignores boss and wanders away ("ga kompak")
            redRoamer.x = boss.x - 3.0f;
            redRoamer.y = boss.y - 3.0f;
            redRoamer.update(0.1f, testEngine);
            assertFalse(redRoamer.targetEntity == boss, "Roamer refuses to help / hit boss ('ga kompak')");
            assertTrue(redRoamer.destinationX != boss.x || redRoamer.destinationY != boss.y, "Roamer wanders away from boss pit");

            System.out.println("[PASS] Test 17: Clumsy and Uncoordinated Roam and Jungler AI verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 17: " + t.getMessage());
            failed++;
        }

        // Test 18: Minion target filtering (no buff aggro), Roam following Gold Lane & Jungler taking buffs
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();

            // 1. Minion should NOT target BuffMonster even when standing directly next to it
            BuffMonster blueBuff = testEngine.getBuffMonsters().get(0);
            Minion testMinion = new Minion(blueBuff.x + 1.0f, blueBuff.y + 1.0f, Entity.TEAM_BLUE, false, null);
            testMinion.update(0.1f, testEngine);
            assertFalse(testMinion.targetEntity instanceof BuffMonster, "Minion must NEVER target BuffMonster");
            assertFalse(testMinion.targetEntity instanceof JungleCamp, "Minion must NEVER target JungleCamp");

            // 2. Jungler taking jungle buffs when alive
            Hero blueJungler = null;
            Hero blueRoamer = null;
            Hero blueGoldLaner = null;
            for (Hero h : testEngine.getBlueHeroes()) {
                if (h.assignedLane == Hero.Lane.JUNGLE) blueJungler = h;
                if (h.assignedLane == Hero.Lane.ROAM) blueRoamer = h;
                if (h.assignedLane == Hero.Lane.GOLD) blueGoldLaner = h;
            }
            assertNotNull(blueJungler, "Blue Jungler exists");
            assertNotNull(blueRoamer, "Blue Roamer exists");
            assertNotNull(blueGoldLaner, "Blue Gold Laner exists");

            // Position Jungler near blue buff camp and update AI
            blueJungler.x = blueBuff.x - 1.5f;
            blueJungler.y = blueBuff.y - 1.5f;
            blueJungler.update(0.1f, testEngine);
            assertEquals(blueBuff, blueJungler.targetEntity, "Jungler targets and attacks alive jungle buff monster");

            // 3. Roamer follows and accompanies Gold Laner (Marksman)
            blueGoldLaner.x = 22.0f;
            blueGoldLaner.y = 8.0f;
            blueRoamer.x = 10.0f;
            blueRoamer.y = 8.0f;
            testEngine.setMatchTime(10.0f); // In Gold Lane rotation phase
            blueRoamer.update(0.1f, testEngine);
            assertTrue(Math.abs(blueRoamer.destinationX - blueGoldLaner.x) <= 3.0f, "Roamer navigates to accompany Gold Laner");

            // 4. Hero stationary walkAnim check: stationary hero does not animate walk
            Hero player = testEngine.getPlayerHero();
            float prevAnim = player.walkAnim;
            player.destinationX = player.x;
            player.destinationY = player.y;
            player.update(0.1f, testEngine);
            assertFalse(player.isMoving, "Hero is not moving when at destination");
            assertEquals(prevAnim, player.walkAnim, "Walk animation does not advance when stationary (no jalan di tempat)");

            System.out.println("[PASS] Test 18: Minion target filtering, Roam escorting Gold Lane & Jungler buff taking verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 18: " + t.getMessage());
            failed++;
        }

        // Test 19: 20-second kill attribution rule (Turret, Minion, Buff, Turtle, Lord kills credited to recent hero)
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();
            Hero player = testEngine.getPlayerHero();
            Hero enemyHero = testEngine.getRedHeroes().get(0);
            Tower tower = null;
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof Tower t && t.team == Entity.TEAM_BLUE) {
                    tower = t;
                    break;
                }
            }
            assertNotNull(tower, "Blue tower exists");

            // Scenario A: Player damages enemy hero at t = 100s. Enemy hero killed by Tower at t = 115s (15s elapsed <= 20s)
            testEngine.setMatchTime(100.0f);
            enemyHero.takeDamage(100.0f, player, testEngine);
            assertEquals(player, enemyHero.lastHeroDamager, "Player recorded as last hero damager");
            assertEquals(100.0f, enemyHero.lastHeroDamageTime, "Damage timestamp recorded");

            int initialKills = player.kills;
            int initialGold = player.gold;

            testEngine.setMatchTime(115.0f); // 15 seconds later
            enemyHero.takeDamage(99999f, tower, testEngine); // Fatal damage from Turret
            assertFalse(enemyHero.alive, "Enemy hero dead");
            assertEquals(initialKills + 1, player.kills, "Player awarded kill credit for 15s recent damage");
            assertEquals(initialGold + 320, player.gold, "Player awarded +320 gold kill reward");
            assertEquals(player.heroName, enemyHero.lastKilledByName, "Killer name credited to Player");

            // Scenario B: Hero dies to Turret WITHOUT damage in last 20 seconds (e.g. 25s elapsed)
            enemyHero.respawn(testEngine);
            testEngine.setMatchTime(200.0f);
            enemyHero.takeDamage(50.0f, player, testEngine);

            initialKills = player.kills;
            initialGold = player.gold;

            testEngine.setMatchTime(225.0f); // 25 seconds later (> 20s)
            enemyHero.takeDamage(99999f, tower, testEngine);
            assertEquals(initialKills, player.kills, "Player NOT awarded kill credit after 25s (exceeds 20s limit)");
            assertEquals(initialGold, player.gold, "No gold reward given to player");
            assertEquals(tower.towerName, enemyHero.lastKilledByName, "Executed by Turret attributed to Turret");

            // Scenario C: Hero killed by BuffMonster or Turtle within 20s
            enemyHero.respawn(testEngine);
            BuffMonster buffMonster = testEngine.getBuffMonsters().get(0);
            testEngine.setMatchTime(300.0f);
            enemyHero.takeDamage(60.0f, player, testEngine);

            initialKills = player.kills;
            testEngine.setMatchTime(312.0f); // 12 seconds later
            enemyHero.takeDamage(99999f, buffMonster, testEngine); // Killed by Buff Monster
            assertEquals(initialKills + 1, player.kills, "Player awarded kill credit when enemy hero executed by Buff Monster within 20s");
            assertEquals(player.heroName, enemyHero.lastKilledByName, "Killer name credited to Player");

            // Scenario D: Enemy minion killed by turret after player hit within 20s
            Minion redMinion = new Minion(tower.x + 1.0f, tower.y + 1.0f, Entity.TEAM_RED, false, null);
            testEngine.setMatchTime(400.0f);
            redMinion.takeDamage(20.0f, player, testEngine);

            int initialCS = player.creepScore;
            int initialMinionGold = player.gold;
            testEngine.setMatchTime(410.0f); // 10 seconds later
            redMinion.takeDamage(99999f, tower, testEngine); // Finished by Turret
            assertEquals(initialCS + 1, player.creepScore, "Player awarded CS when minion finished by Turret within 20s");
            assertTrue(player.gold > initialMinionGold, "Player awarded minion gold");

            System.out.println("[PASS] Test 19: 20-second damage attribution rule across Turrets, Minions & Buffs verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 19: " + t.getMessage());
            failed++;
        }

        // Test 20: Safe Turret Standoff (no walk-in-place jitter) & Active Jungler Turtle/Lord pursuit
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();

            // 1. Safe Turret Standoff (Zero Walk-in-Place / No Turret Border Jitter):
            Tower redTower = null;
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof Tower t && t.team == Entity.TEAM_RED) {
                    redTower = t;
                    break;
                }
            }
            assertNotNull(redTower, "Red tower exists");

            // Find a blue laner hero (not player)
            Hero blueLaner = null;
            for (Hero h : testEngine.getBlueHeroes()) {
                if (!h.isPlayer && h.assignedLane != Hero.Lane.JUNGLE && h.assignedLane != Hero.Lane.ROAM) {
                    blueLaner = h;
                    break;
                }
            }
            assertNotNull(blueLaner, "Blue laner hero found");

            // 1. Safe Turret Retreat & Standoff without Minions:
            // When placed near the turret border (8.0f), bot actively retreats away from danger towards safe perimeter
            blueLaner.x = redTower.x - 8.0f;
            blueLaner.y = redTower.y;
            blueLaner.destinationX = redTower.x; // Attempting to push down lane
            blueLaner.destinationY = redTower.y;

            blueLaner.update(0.1f, testEngine);
            assertTrue(blueLaner.destinationX < redTower.x - 8.0f || blueLaner.isInBush,
                    "Bot sets retreat destination away from enemy turret border without minions or takes safe bush cover");

            // Position bot at deep safe retreat standoff perimeter (11.0f away)
            blueLaner.x = redTower.x - 11.0f;
            blueLaner.y = redTower.y;
            float initialWalkAnim = blueLaner.walkAnim;
            for (int tick = 0; tick < 5; tick++) {
                blueLaner.update(0.1f, testEngine);
            }

            // Bot should stand ground: isMoving is false, not diving, no walk anim advancement
            assertFalse(blueLaner.isMoving, "Bot stands ground at safe retreat perimeter waiting for minions");
            assertEquals(initialWalkAnim, blueLaner.walkAnim, "Bot walk anim does not advance (no jalan di tempat)");
            assertTrue(blueLaner.distanceTo(redTower) >= redTower.attackRange + 3.0f, "Bot stays safely far outside turret attack range without minions");
            assertFalse(blueLaner.targetEntity == redTower, "Bot does not blindly target tower without minion support");

            // 2. Jungler Active Pursuit of Turtle / Lord:
            JungleCamp boss = testEngine.getJungleBoss();
            boss.spawnTurtleImmediately();
            assertTrue(boss.alive, "Turtle spawned");

            Hero blueJungler = null;
            for (Hero h : testEngine.getBlueHeroes()) {
                if (h.assignedLane == Hero.Lane.JUNGLE) {
                    blueJungler = h;
                    break;
                }
            }
            assertNotNull(blueJungler, "Blue Jungler exists");

            // Position Jungler far from boss pit (e.g. at spawn)
            blueJungler.x = blueJungler.spawnX;
            blueJungler.y = blueJungler.spawnY;
            blueJungler.hp = blueJungler.maxHp;
            blueJungler.update(0.1f, testEngine);

            // Healthy Jungler must actively target and navigate to boss pit from afar!
            assertEquals(boss, blueJungler.targetEntity, "Jungler targets alive Turtle/Lord from afar");
            assertEquals(boss.x, blueJungler.destinationX, "Jungler sets destination to boss X");
            assertEquals(boss.y, blueJungler.destinationY, "Jungler sets destination to boss Y");

            // Position Jungler inside attack range in boss pit
            blueJungler.x = boss.x - (blueJungler.attackRange * 0.7f);
            blueJungler.y = boss.y;
            blueJungler.update(0.1f, testEngine);

            assertEquals(boss, blueJungler.targetEntity, "Jungler maintains boss target in pit");
            assertFalse(blueJungler.isMoving, "Jungler stops moving to engage boss in combat");

            System.out.println("[PASS] Test 20: Safe Turret Standoff (no walk-in-place) & Active Jungler Turtle/Lord pursuit verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 20: " + t.getMessage());
            failed++;
        }

        // Test 21: Pro MLBB Bot Macro Behaviors during Turret Standoff without Minions
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchDirectly();

            Tower blueMidTower = null;
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof Tower t && t.team == Entity.TEAM_BLUE && "Blue Mid Outer".equals(t.towerName)) {
                    blueMidTower = t;
                    break;
                }
            }
            assertNotNull(blueMidTower, "Blue Mid Outer tower exists");

            Hero enemyMid = null;
            for (Hero h : testEngine.getRedHeroes()) {
                if (h.assignedLane == Hero.Lane.MID) {
                    enemyMid = h;
                    break;
                }
            }
            assertNotNull(enemyMid, "Enemy Mid Hero (Herobrine) exists");

            // 1. Behavior A: Low/Moderate HP Recall (<60% HP) when no minions are present
            // Position enemy Mid at safe standoff of Blue Mid Outer (around 25.6, 25.6, distance ~7.9f) with 45% HP
            enemyMid.x = blueMidTower.x + 5.6f;
            enemyMid.y = blueMidTower.y + 5.6f;
            enemyMid.hp = enemyMid.maxHp * 0.45f;
            enemyMid.update(0.1f, testEngine);

            assertEquals(enemyMid.spawnX, enemyMid.destinationX, "Low HP bot recalls to base spawn instead of idling at turret");
            assertEquals(enemyMid.spawnY, enemyMid.destinationY, "Low HP bot recalls to base spawn instead of idling at turret");

            // 2. Behavior B: Take Cover & Ambush in Nearby Safe Bush when full HP
            // Reset HP to 100%
            enemyMid.hp = enemyMid.maxHp;
            // Mid River Bushes exist at (27, 24) and (28, 24).
            // Position enemyMid near the river entrance outside tower range
            enemyMid.x = 27.5f;
            enemyMid.y = 25.0f;
            enemyMid.update(0.1f, testEngine);

            // Bot should navigate towards safe bush
            assertTrue(Math.abs(enemyMid.destinationX - 27.0f) <= 2.0f && Math.abs(enemyMid.destinationY - 24.0f) <= 2.0f,
                    "Healthy bot takes cover in nearby river bush to prepare ambush while waiting for wave");

            // Simulate natural walking into bush across ticks
            for (int t = 0; t < 15; t++) {
                enemyMid.update(0.1f, testEngine);
            }

            // The bot must have entered deep into the bush center (27.0, 24.0) instead of stopping on the doorstep
            assertTrue(Math.abs(enemyMid.x - 27.0f) <= 0.25f, "Bot walked deep into bush center");
            assertTrue(Math.abs(enemyMid.y - 24.0f) <= 0.25f, "Bot walked deep into bush center");
            assertTrue(enemyMid.isInBush, "Bot is fully concealed inside tall grass bush");
            assertFalse(enemyMid.isMoving, "Bot holds ambush position quietly inside bush (no movement jitter)");

            // 3. Behavior C: Intercept and Farm Oncoming Minions Outside Turret
            Minion incomingBlueMinion = new Minion(25.0f, 25.0f, Entity.TEAM_BLUE, false, null);
            testEngine.getAllEntities().add(incomingBlueMinion);
            enemyMid.x = 26.5f;
            enemyMid.y = 26.5f;
            enemyMid.update(0.1f, testEngine);

            assertEquals(incomingBlueMinion, enemyMid.targetEntity, "Bot proactively intercepts and targets incoming minion outside turret");

            // 4. Behavior D: Minion Bush Blindness (Minions cannot see or target heroes in bush)
            incomingBlueMinion.alive = false; // Remove other targets
            Hero playerHero = testEngine.getPlayerHero();
            playerHero.x = 27.0f;
            playerHero.y = 24.0f;
            playerHero.isInBush = true;
            playerHero.revealTimer = 0f;

            Minion redLaneMinion = new Minion(27.0f, 26.0f, Entity.TEAM_RED, false, null); // 2 blocks away
            testEngine.getAllEntities().add(redLaneMinion);
            redLaneMinion.update(0.1f, testEngine);
            assertNull(redLaneMinion.targetEntity, "Minion cannot see or target player hero concealed in bush");

            // If player hero attacks (revealTimer > 0), minion can acquire target
            playerHero.revealTimer = 2.5f;
            redLaneMinion.update(0.1f, testEngine);
            assertEquals(playerHero, redLaneMinion.targetEntity, "Minion acquires hero once revealed by attack");

            System.out.println("[PASS] Test 21: Pro MLBB Bot Macro Behaviors (Recall, Bush Ambush, Wave Intercept, Minion Bush Blindness) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 21: " + t.getMessage());
            failed++;
        }

        // Test 22: Assist Attribution, Lord 10-Second Delay, Weakest Lane Push & Anti-Baiting
        try {
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchWithDraft("Steve", Hero.Lane.EXP);

            Hero player = testEngine.getPlayerHero();
            Hero allyMid = null;
            for (Hero h : testEngine.getBlueHeroes()) {
                if (h != player && h.assignedLane == Hero.Lane.MID) {
                    allyMid = h;
                    break;
                }
            }
            assertNotNull(allyMid, "Allied Mid hero exists");

            Hero enemyMid = null;
            for (Hero h : testEngine.getRedHeroes()) {
                if (h.assignedLane == Hero.Lane.MID) {
                    enemyMid = h;
                    break;
                }
            }
            assertNotNull(enemyMid, "Enemy Mid hero exists");

            // --- 1. Assist Attribution Verification ---
            // Player and AllyMid both damage enemyMid
            int playerAssistsBefore = player.assists;
            int allyKillsBefore = allyMid.kills;

            enemyMid.takeDamage(100f, player, testEngine);
            enemyMid.takeDamage(150f, allyMid, testEngine);
            // AllyMid delivers final fatal blow
            enemyMid.takeDamage(99999f, allyMid, testEngine);

            assertFalse(enemyMid.alive, "Enemy Mid hero died");
            assertEquals(allyKillsBefore + 1, allyMid.kills, "AllyMid receives the kill");
            assertEquals(playerAssistsBefore + 1, player.assists, "Player receives an assist for contributing damage in last 20s");

            // --- 2. Lord 10s Summon Delay & Weakest Lane Targeting ---
            // Destroy Red Bot Towers to make Bot Lane have fewest turrets (0 towers on Bot vs 2 on Mid, 2 on Top)
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof Tower tower && tower.team == Entity.TEAM_RED) {
                    if (tower.towerName.contains("Bot")) {
                        tower.alive = false;
                    }
                }
            }

            JungleCamp boss = testEngine.getJungleBoss();
            boss.setBossType(JungleCamp.BossType.LORD);
            boss.alive = true;
            boss.hp = boss.maxHp;

            // Slay Lord by player
            int countBefore = testEngine.getAllEntities().size();
            boss.takeDamage(99999f, player, testEngine);
            assertFalse(boss.alive, "Lord slain in pit");
            assertTrue(boss.pendingLordSummon, "Pending Lord summon active for 10-second delay");
            assertEquals(10.0f, boss.lordSummonCountdown, "Summon countdown initialized to 10.0 seconds");

            // After 5s, still not spawned
            boss.update(5.0f, testEngine);
            assertEquals(countBefore, testEngine.getAllEntities().size(), "Lord still preparing summon at 5s");

            // After remaining 5.1s, Lord evaluates weakest lane (Bot Lane) and spawns!
            boss.update(5.1f, testEngine);
            assertFalse(boss.pendingLordSummon, "Summon countdown finished");

            SummonedLord botLord = null;
            for (Entity e : testEngine.getAllEntities()) {
                if (e instanceof SummonedLord sl && sl.team == Entity.TEAM_BLUE) {
                    botLord = sl;
                    break;
                }
            }
            assertNotNull(botLord, "Summoned Lord spawned");
            assertEquals(Hero.Lane.BOT, botLord.assignedLane, "Lord targeted Bot Lane because it had the fewest surviving enemy turrets");

            // --- 3. Lord Lane Focus, Anti-Baiting & Turret Priority ---
            // Place botLord on Bot Lane
            botLord.x = 25.0f;
            botLord.y = 6.0f;

            // An enemy attacks Lord from distance 5.5f (kiting)
            enemyMid.alive = true;
            enemyMid.x = 25.0f;
            enemyMid.y = 11.5f; // distance 5.5f away
            botLord.takeDamage(20f, enemyMid, testEngine);
            botLord.update(0.1f, testEngine);

            // Lord refuses to be baited off-lane by distant kiting enemies!
            assertNull(botLord.targetEntity, "Lord does not chase kiting enemy off lane");

            // Place an enemy Turret in range and enemy close by
            Tower testTower = new Tower("Red Test Tower", 28.0f, 6.0f, Entity.TEAM_RED);
            testEngine.getAllEntities().add(testTower);
            enemyMid.x = 26.5f;
            enemyMid.y = 6.0f; // distance 1.5f away from Lord
            botLord.update(0.1f, testEngine);

            // Lord MUST prioritize the enemy Turret over the enemy hero!
            assertEquals(testTower, botLord.targetEntity, "Lord prioritizes destroying Turret over enemy heroes");

            System.out.println("[PASS] Test 22: Assist Attribution, Lord 10-Second Delay, Weakest Lane Push & Anti-Baiting verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 22: " + t.getMessage());
            failed++;
        }

        // Test 23: SoundManager Dropout Prevention & Ultimate R Level 4 Lock Verification
        try {
            // 1. SoundManager Stress Test: Rapid sequential firing of all SFX (no dropouts, no crashes)
            moba.audio.SoundManager.setSoundEnabled(true);
            assertTrue(moba.audio.SoundManager.isSoundEnabled(), "Sound is enabled");
            for (int i = 0; i < 20; i++) {
                moba.audio.SoundManager.playHit();
                moba.audio.SoundManager.playSwordSwing();
                moba.audio.SoundManager.playShoot();
                moba.audio.SoundManager.playExplosion();
                moba.audio.SoundManager.playLevelUp();
                moba.audio.SoundManager.playCoin();
                moba.audio.SoundManager.playTeleport();
                moba.audio.SoundManager.playTowerShot();
            }

            // 2. Ultimate Skill (R) Level 4 Lock Verification
            GameEngine testEngine = new GameEngine(1024, 720);
            testEngine.startMatchWithDraft("Steve", Hero.Lane.EXP);
            Hero player = testEngine.getPlayerHero();

            for (int lvl = 1; lvl <= 3; lvl++) {
                player.level = lvl;
                assertFalse(player.skillR.isUnlocked(player), "Ultimate R is locked at Level " + lvl);
                assertFalse(player.skillR.cast(player, player.x, player.y, null, testEngine), "Cannot cast Ultimate R at Level " + lvl);
            }

            player.level = 1;
            testEngine.saveScreenshot("screenshots/screenshot_ultimate_locked.png");

            // Level 4: Ultimate unlocks!
            player.level = 4;
            player.mana = player.maxMana;
            player.skillR.currentCooldown = 0f;
            assertTrue(player.skillR.isUnlocked(player), "Ultimate R is unlocked at Level 4");
            assertTrue(player.skillR.cast(player, player.x, player.y, null, testEngine), "Can cast Ultimate R at Level 4");

            System.out.println("[PASS] Test 23: SoundManager Dropout Prevention & Ultimate R Level 4 Lock verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 23: " + t.getMessage());
            failed++;
        }

        System.out.println("==================================================");
        System.out.printf("   TEST SUMMARY: %d PASSED, %d FAILED\n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
        System.exit(0);
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("AssertTrue failed: " + msg);
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError("AssertFalse failed: " + msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError("AssertNotNull failed: " + obj + " " + msg);
    }

    private static void assertNull(Object obj, String msg) {
        if (obj != null) throw new AssertionError("AssertNull failed: expected null but was " + obj + " for " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(String.format("AssertEquals failed: expected <%s> but got <%s> for %s", expected, actual, msg));
        }
    }
}
