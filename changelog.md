2.0.0 - ETERNAL IRON SHELL Prerelease

New ship - Dauntless (XIV)
New ship - Illustrious (XIV)

\-Various edits to character writing, dialog, and interactions.
-Contact rewards adjusted:
-Ava now grants a second round of training, granting a reworked elite upgrade to the Iron Heritage skill, as well as a ship reward.
-Charlotte is now finally recruitable, with the new Decisive Strike skill, a hybrid energy weapon mastery, on top of Iron Heritage.
-Hartley now has a "XIV hull conversion" reward, letting you turn Champion and Eradicator into their XIV variants, as well as a ship reward.
-Caeda has a new reward based around a certain set of automated XIV vessels.
-Added an inactive gate to Naraka. (requires new save)
-Chitagupta texture updated. Now with 100% more pink. (requires new save)
-Iron shell comission now has identical functionality for vanilla hegemony comission checks.
-Iron shell now piggybacks nanoforge quality off of the hegemony.

\-Slightly reworked raid response fleets. Can now story point flee all besides Chico response fleet.
-Added new special non-sp and sp flee options to Ava's previously un-sp-able Chico raid response fleet.
-Barracuda Balance Container: weight 100000 -> 1000000 (but also removed mission\_item tag, so it's removable from your cargo hold now)
-Greater Hegemony (perma alliance) toggle, and noEISplayer (portraits) to lunalib settings rather than in settings.json



SHIPS:

Tyrant
-Weapon arcs adjusted
-shield eff 1.2 -> 1.0
-OP 325 -> 310
-gained HBI
-fluxthrower built-in removed
-prow hardpoint -> large composite

Vengence
-2 small ballistic -> medium energy
-Aft med ballistic -> hybrid
-shield eff 1.0 -> 0.8
-Flux diss 450 -> 600
-OP 160 -> 180
-Speed 75->80
-now has missile microforge built in
-Removed debuff for a mis-timed system usage.
-Buff for a successful parry changed, now is a 4 second, 50% ROF boost.
-Revengeance ship system cooldown now benefits from system cooldown buffs such as system expertise.
-No longer grants flux capacity when using neural interface.

Audacious
-3 small ballistic -> hybrid
-Audacious relay hullmod 25% shield bonus -> 10% shield bonus within 1000su of a Vengence
-shield eff 1.1 -> 0.8
-flux cap 6000->5500
-No longer grants flux dissipation when using neural interface

Renown
-"Renown Mounts" hullmod removed
-Prow large energy hardpoint -> large synergy
-Now has a similar "cramped hull" hull mod to the indomitable. Mounting both a large missile and medium missile will incur debuffs.
-Added 1 gunshield wing as a built in fighter
-Speed 75->80

Courageous
-Flux capacity 2100 -> 3000
-Flux dissipation 180 -> 200
-Armor 500 -> 400
-System: now deals HE -> energy damage with values matching termination sequence.

Valorous
-Fixed a major bug that resulted in the shield effectiveness on parry to be an 80% buff (5x less shield damage taken) rather than the displayed 20%.
-Shield damage reduction on parry buffed from 20% to 30% (this is a major functional nerf combined with the bugfix)
-Removed converted hanger option
-Ammo regen now applies without converted hanger
-Rapid ammo feeder buff now is all non-missile weapons (slots unchanged, just now supports "hybrids that count as energy")
-System regen speed buffs are now 100% effective - removed previous sysex debuff
(the bugfix is a major change, the ship may need to be buffed in the future as a result)

Skysplitter (XIV)
-(DP), supplies/rec, supplies/mo 35 -> 40



FIGHTERS:
-All fighter wings have been overhauled
-Iron Shell fighters are now primarily treated as premium variants of their vanilla counterparts



WEAPONS:

Azalean Lance
-added USE\_LESS\_VS\_SHIELDS hint
-adjuststed damage/flux, now has 276 dps @ 1.0 flux eff
-Adjusted coolup/cooldown, now should be more effective at breaking armor

Superheavy needler
-Reworked, no longer an ammo weapon
-Now fires a 60 round burst at double the rate of fire.
-Damage per projectile 65->50
-Refire delay now 6sec
-Flux/dam 0.77->0.8
-Sustained dps now 500
-OP 28->25
-Accuracy very poor -> medium (similar to light/heavy needler)
Note: this weapon was originally made before the vanilla storm needler became an ammo weapon. As a result, it became a worse version of the storm needler. This change aims to return it to being the heavy burst option, while storm needler has signifigantly higher sustained dps on ships with expanded magazines.

Celestial aster/blossom
-OP reduced from 12/28 to 10/22
-Damage per shot increased from 900 to 1000

Lunar rose MIRV
-Range increased from 1500 -> 2000



HULLMODS:

Missile Microforge
-Now no longer pauses when overloaded/venting/high flux
-Removed eradicator-specific microforge nerf, now same for all hulls.

Vanagloria Ionized Armor
-Remade into a magic subsystem
-Now may be installed on civilian/frigates, no longer can be installed on phase ships
-Removed a formula adjusting recharge time, now a fixed 20 seconds.
-Duration is now 3s for all hull sizes
-Removed smod bonus of -25% recharge, now has smod penalty of +25% recharge
-Adjusted cost to 8/12/20/30 OP

Gula Tandom Warheads
-Completely reworked. Now buffs non-guided missile rof, speed, regen.
-Smod effect now applies these bonuses to fighters regardless of guidance, and increases speed of autoforge.

Avaritia Capacity Overhaul
-S-mod bonus changed to 10% vent rate
-Buff duration is now 3s for all hull sizes, previously 2/2.25/2.5/3s
-Now may be mounted on capital ships

Aquila Reactor
-hullmod OP cost adjusted, now matches ITU



SYSTEMS:

Anihihilation protocol (Skysplitter XIV/Renown system)
-Max uses 1->2
-Charge up/down 0.5->0.25s, duration 5s->6s (total duration 6s->6.5s)
-Adjusted effect to match description:
-Ballistic ROF now expires alongside energy damage after 3s (previously remained at half strength for remaining system duration)
-Damage boost now correctly stays on for 3s of the active duration (previously only 2.5s)
-Ballistic flux reduction now properly offsets ROF increase

Damper subfield (XIV mora system)
-System reworked. Now reduces damage taken by 40% for the ship and fighters (was 50% for fighters only)
-No longer uses charges. Cooldown remains 20s
-Active duration now 3s (was 6s)
-Visual effect adjusted, now more closely matches damper field.

Jump thruster (audacious/endeavor system)
-No longer has a 0.5s delay. SFX adjusted.
-Jump adjusted to approximately 1.5x the strength
-Forward movement now matches lateral movement. Reverse movement is still less effective.



MISC:
-Fixed the Indomitable supership start not correctly giving free built-in Vanagloria Ionized Armor
-Updated main menu mission descriptions to make it more clear what the custom starts they grant are.


1.183.5b Patch Notes (discord)

\-Fixed version checker files. Maybe. idkkkk

\-\[Buff] Switched Rampage Drive system to Burn Drive AI; decision-making \& survivability is improved

&#x20;  - Affects: Vanguard (XIV), Relentless, Indomitable



Indomitable

\-\[Buff] Adjusted AI to encourage missile use



Champion (XIV)

\-\[Buff] Ordnance points increased from 170 to 190

\-\[Buff] Flux dissipation increased from 550 to 650

&#x20;  -now matches upcoming vanilla Champ buffs



Renown

\-\[Buff] Top speed increased from 65 to 75

\-\[Buff] Ordnance points increased from 155 to 165

\-\[Buff] Flux dissipation increased from 450 to 550

&#x20;  -this ship is heavily based off of Champion, so she gets some buffs as well. Speed increased to (partially..) compensate for the loss of built-in Aquila.



1.183.5a Patch Notes (discord)

Flagellator

\-replaced with Relentless-class heavy destroyer

&#x20;  -The updated ship is too far removed from the original Flagellator to really be considered a reskin

&#x20;  -Old flagellator (XIV) sprites may be freely used by anyone for any starsector project

&#x20;  

Relentless

\-\[Buff] mounts \& layout drastically improved

\-\[Buff] Shield arc raised from 90 to 120

\-\[Buff] flux dissipation increased from 300 to 350

\-\[Nerf] DP raised from 10 to 12

\-\[Nerf] Removed modular fighter bay

\-\[Nerf] Removed built-in PD drones

\-Adjusted variants



Vanguard

\-\[Buff] Added built-in Armored Weapon Mounts

\-\[Note] As the baseline Vanguard is being reduced to 4 DP in the upcoming 0.98.5 update, the Iron Shell variant is being treated as a more premium option with drastically improved survivability



Tyrant

\-Adjusted variants

\-\[Buff] Ordnance Points increased from 310 to 325

\-Removed built-in Aquila Reactor Protocol hullmod

&#x20;  -should be more flexible re: fleet composition



Renown

\-Adjusted variants

\-\[Buff] Ordnance Points increased from 145 to 155

\-Removed built-in Aquila Reactor Protocol hullmod



\-\[Nerf] Cooldown added to Rampage drive

\-\[Nerf] Rampage Drive charge count reduced from 2 to 1

\-\[Buff] Rampage Drive can now be cancelled

\-Added Rugged Construction to Indomitable-class cruiser

&#x20;  -god's most suicidal soldier

\-Gunblade-class PD replacement time reduced from 18 to 10 seconds to match 0.98 terminator drone changes

\-Updated Celestial's Aster weapon sprite

\-Fixed Amity fighter engine \& weapon visuals

