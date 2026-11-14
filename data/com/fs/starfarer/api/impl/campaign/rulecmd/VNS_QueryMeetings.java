package com.fs.starfarer.api.impl.campaign.rulecmd;

/**
 * EIS_DynamicPaginatedOptions with 5 options per page instead of 6. Same arguments.
 * Not used by any rules.csv row at the moment.
 *
 * This used to extend Nexerelin's PaginatedOptionsPlus with its own copy of the option queues. It now shares
 * EIS_DynamicPaginatedOptions (and its queues) so the class loads without Nexerelin installed.
 */
public class VNS_QueryMeetings extends EIS_DynamicPaginatedOptions {
	public VNS_QueryMeetings() {
		super();
		optionsPerPage = 5;
	}
}
