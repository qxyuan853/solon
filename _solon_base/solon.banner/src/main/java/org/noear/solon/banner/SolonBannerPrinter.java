package org.noear.solon.banner;

import org.noear.solon.Solon;

/**
 * @author pmg1991
 * @since 1.11
 * */
class SolonBannerPrinter{

	private static final String[] BANNER = {  "              ____________________ ____________   __"
											, "              __  ___/_  __ \\__  / __  __ \\__  | / /"
											, "              _____ \\_  / / /_  /  _  / / /_   |/ / "
											, "              ____/ // /_/ /_  /___/ /_/ /_  /|  /  "
											, "              /____/ \\____/ /_____/\\____/ /_/ |_/   "
											, "                                                         ", };

	private static final String SOLON_STR = " ~~ Solon ~~ ";

	private static final int STRAP_LINE_SIZE = 57;


	public static String printBanner() {
		StringBuilder sb = new StringBuilder();
		for (String line : BANNER) {
			sb.append(line).append("\r\n");
		}
		String version = Solon.cfg().version();
		
		version = (version != null) ? " (v" + version + ")" : "";
	
		
		StringBuilder padding = new StringBuilder();
		while (padding.length() < STRAP_LINE_SIZE - (version.length() + SOLON_STR.length())) {
			padding.append(" ");
		}

				
		sb.append(SOLON_STR).append(padding.toString()).append(version).append("\r\n");;
		return sb.toString();
	}

}