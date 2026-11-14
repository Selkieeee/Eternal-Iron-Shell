package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DevMenuOptions;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paginated options where several rules rows add options before they are shown. Runs the contact menus.
 *
 * Iron Shell's own copy of Nexerelin's Nex_DynamicPaginatedOptions and its parent PaginatedOptionsPlus
 * (c) Histidine, MIT license, so the menus work without Nexerelin installed. Same arguments as the original:
 *
 * EIS_DynamicPaginatedOptions addOption <option id> <text>
 * EIS_DynamicPaginatedOptions addOptionAllPages <option id> <text>
 * EIS_DynamicPaginatedOptions setColor <option id> <color>
 * EIS_DynamicPaginatedOptions addTooltip <option id> <text>
 * EIS_DynamicPaginatedOptions addTooltipHighlights <option id> <text...>
 * EIS_DynamicPaginatedOptions addTooltipHighlightColors <option id> <color...>
 * EIS_DynamicPaginatedOptions addShortcut <option id> <key name>
 * EIS_DynamicPaginatedOptions show
 */
public class EIS_DynamicPaginatedOptions extends PaginatedOptions {

	public static final int OPTIONS_PER_PAGE = 6;	// not counting next/previous page and 'go back'

	protected static final List<Misc.Token> EMPTY_PARAMS = new ArrayList<>();

	// Filled by the add* calls, moved into this instance and cleared by "show".
	protected static List<PaginatedOption> optionsSt = new ArrayList<>();
	protected static List<PaginatedOption> optionsAllPagesSt = new ArrayList<>();
	protected static Map<String, String> optionTooltipsSt = new HashMap<>();
	protected static Map<String, Color> optionColorsSt = new HashMap<>();
	protected static Map<String, List<String>> tooltipHighlightsSt = new HashMap<>();
	protected static Map<String, List<Color>> tooltipHighlightColorsSt = new HashMap<>();
	protected static Map<String, Integer> optionShortcutsSt = new HashMap<>();

	protected Map<String, String> optionTooltips = new HashMap<>();
	protected Map<String, Color> optionColors = new HashMap<>();
	protected Map<String, List<String>> tooltipHighlights = new HashMap<>();
	protected Map<String, List<Color>> tooltipHighlightColors = new HashMap<>();
	protected Map<String, Integer> optionShortcuts = new HashMap<>();

	public EIS_DynamicPaginatedOptions() {
		super();
		optionsPerPage = OPTIONS_PER_PAGE;
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		this.dialog = dialog;
		String arg = params.get(0).getString(memoryMap);
		switch (arg)
		{
			case "show":
				super.execute(ruleId, dialog, EMPTY_PARAMS, memoryMap);
				showOptions();
				return true;
			case "addOption":
				optionsSt.add(new PaginatedOption(params.get(2).getStringWithTokenReplacement(ruleId, dialog, memoryMap), params.get(1).getString(memoryMap)));
				return true;
			case "addOptionAllPages":
				optionsAllPagesSt.add(new PaginatedOption(params.get(2).getStringWithTokenReplacement(ruleId, dialog, memoryMap), params.get(1).getString(memoryMap)));
				return true;
			case "addColor":
			case "setColor":
				optionColorsSt.put(params.get(1).getString(memoryMap), params.get(2).getColor(memoryMap));
				return true;
			case "addTooltip":
				optionTooltipsSt.put(params.get(1).getString(memoryMap), params.get(2).getStringWithTokenReplacement(ruleId, dialog, memoryMap));
				return true;
			case "addTooltipHighlights":
				List<String> strings = new ArrayList<>();
				for (int i = 2; i < params.size(); i++) {
					String string = params.get(i).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
					if (string != null) strings.add(string);
				}
				tooltipHighlightsSt.put(params.get(1).getString(memoryMap), strings);
				return true;
			case "addTooltipHighlightColors":
				List<Color> colors = new ArrayList<>();
				for (int i = 2; i < params.size(); i++) {
					colors.add(params.get(i).getColor(memoryMap));
				}
				tooltipHighlightColorsSt.put(params.get(1).getString(memoryMap), colors);
				return true;
			case "addShortcut":
				int code = Global.getSettings().getCodeFor(params.get(2).getString(memoryMap));
				if (code == -1) return false;
				optionShortcutsSt.put(params.get(1).getString(memoryMap), code);
				return true;
		}
		return false;
	}

	protected void processOption(PaginatedOption option) {
		OptionPanelAPI opts = dialog.getOptionPanel();
		String optId = option.id;
		String tooltip = optionTooltips.containsKey(optId) ? optionTooltips.get(optId) : null;

		if (optionColors.containsKey(optId))
			opts.addOption(option.text, optId, optionColors.get(optId), tooltip);
		else {
			opts.addOption(option.text, optId);
			opts.setTooltip(optId, tooltip);
		}

		if (tooltipHighlights.containsKey(optId)) {
			opts.setTooltipHighlights(optId, tooltipHighlights.get(optId).toArray(new String[0]));
		}
		if (tooltipHighlightColors.containsKey(optId)) {
			opts.setTooltipHighlightColors(optId, tooltipHighlightColors.get(optId).toArray(new Color[0]));
		}
		if (optionShortcuts.containsKey(optId)) {
			opts.setShortcut(optId, optionShortcuts.get(optId), false, false, false, false);
		}
	}

	@Override
	public void showOptions() {
		// Take over whatever the add* calls queued up, then clear the queues for the next menu.
		options.addAll(optionsSt);
		optionsAllPages.addAll(optionsAllPagesSt);
		optionTooltips.putAll(optionTooltipsSt);
		optionColors.putAll(optionColorsSt);
		tooltipHighlights.putAll(tooltipHighlightsSt);
		tooltipHighlightColors.putAll(tooltipHighlightColorsSt);
		optionShortcuts.putAll(optionShortcutsSt);
		optionsSt.clear();
		optionsAllPagesSt.clear();
		optionTooltipsSt.clear();
		optionColorsSt.clear();
		tooltipHighlightsSt.clear();
		tooltipHighlightColorsSt.clear();
		optionShortcutsSt.clear();

		OptionPanelAPI opts = dialog.getOptionPanel();
		opts.clearOptions();

		int maxPages = (int) Math.ceil((float)options.size() / (float)optionsPerPage);
		boolean singlePageMode = false;
		// Use the space for the back/forward buttons to fit more options instead, if we can
		if (options.size() + optionsAllPages.size() <= optionsPerPage + 2) {
			maxPages = 1;
			singlePageMode = true;
		}

		if (currPage > maxPages - 1) currPage = maxPages - 1;
		if (currPage < 0) currPage = 0;

		int start = currPage * optionsPerPage;
		int end = start + optionsPerPage;
		if (singlePageMode) end = optionsPerPage + 2;

		for (int i = start; i < end; i++) {
			if (i >= options.size()) {
				if (maxPages > 1 && withSpacers) {
					opts.addOption("", "spacer" + i);
					opts.setEnabled("spacer" + i, false);
				}
			} else {
				processOption(options.get(i));
			}
		}

		if (maxPages > 1) {
			opts.addOption(getPreviousPageText(), OPTION_PREV_PAGE);
			opts.addOption(getNextPageText(), OPTION_NEXT_PAGE);

			if (currPage >= maxPages - 1) {
				opts.setEnabled(OPTION_NEXT_PAGE, false);
			}
			if (currPage <= 0) {
				opts.setEnabled(OPTION_PREV_PAGE, false);
			}
		}

		for (PaginatedOption option : optionsAllPages) {
			processOption(option);
		}

		if (Global.getSettings().isDevMode()) {
			DevMenuOptions.addOptions(dialog);
		}
	}
}
