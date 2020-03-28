package org.cutre.soft.epi.command;

/**
 * Class that represents a Command of type Command. This type of 
 * command accepts three subcommands : 
 * -- ONCE (the command BEGIN and END are consecutively executed
 * -- BEGIN (the commande is activated and hold activated until and END is sent)
 * -- END (after a BEGIN command there must be a END command sent.) 
 * 
 * Copyright (C) 2015  Pau G. - ESHome33
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Pau G. - ESHome33
 * 
 */

public class CmdCommand implements CommandMessage {

	public enum SUBCOMMAND {
		ONCE,
		BEGIN,
		END
	}
	
	private SUBCOMMAND mySubCommand;
	private String CmdName;
	private static String COMMAND_PREFIX = "cmd";
	private static String ONCE_SUB_COMMAND = "once";
	private static String BEGIN_SUB_COMMAND = "begin";
	private static String END_SUB_COMMAND = "end";
	private static String ERROR_SUB_COMMAND = "error";
	
	
	
	/**
	 * Constructs a new Command Message ready to be sent to ExtPlane
	 * @param mySubCommand the subcommand : ONCE, BEGIN, END
	 * @param cmdName the Name of the command. Eg "sim/annunciator/test_all_annunciators"
	 */
	public CmdCommand(SUBCOMMAND mySubCommand, String cmdName) {
		super();
		this.mySubCommand = mySubCommand;
		CmdName = cmdName;
	}



	public String getCommand() {
		return this.buildCommand();
	}



	private String buildCommand() {
		StringBuilder sb = new StringBuilder();

		sb.append(CmdCommand.COMMAND_PREFIX)
			.append(" ")
			.append(this.getSubCommand())
			.append(" ")
			.append(CmdName);
		
		return sb.toString();
	}



	private String getSubCommand() {
		String resu;
		if (mySubCommand == SUBCOMMAND.ONCE) {
			resu = ONCE_SUB_COMMAND;
		} else if (mySubCommand == SUBCOMMAND.BEGIN) {
			resu = BEGIN_SUB_COMMAND;
		} else if (mySubCommand == SUBCOMMAND.END) {
			resu = END_SUB_COMMAND;
		} else {
			resu = ERROR_SUB_COMMAND;
		}
		return resu;
	}

}
