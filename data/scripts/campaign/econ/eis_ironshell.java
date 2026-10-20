package data.scripts.campaign.econ;

import exerelin.campaign.econ.faction.BaseFactionConditionSubplugin;

/**
 * Save-compatibility stub. Do not delete or rename.
 *
 * Older Iron Shell builds (e.g. 1.183.5b) set this class as Iron Shell's Nexerelin "factionConditionSubplugin"
 * (data/config/exerelinFactionConfig/ironshell.json). Nexerelin's FactionConditionPlugin stores its subplugin
 * object in the save, so saves made with those builds contain a serialized
 * data.scripts.campaign.econ.eis_ironshell on each Iron Shell market, and cannot load if this class is missing.
 *
 * The old version had no gameplay effect. It only gave Iron Shell markets a condition named after the faction,
 * with the faction crest as its icon and an expandable lore tooltip: the market's industries protected by the
 * Chicomoztoc Convention (Treaty of Crom Cruach), and High Hegemon Baikal Daud's proclamation transferring the
 * market's governorship to Councilor Kim Quy and Iron Shell.
 *
 * That functionality is intentionally not restored, and the factionConditionSubplugin line in ironshell.json stays
 * commented out. After an old save loads, Nexerelin sees the faction config no longer names this class and drops it.
 */
public class eis_ironshell extends BaseFactionConditionSubplugin {
}
