package screach.titanium.core.cmdparser;

import screach.titanium.core.server.Server;
import screach.titanium.core.server.ServerException;

public abstract class CommandParser {
	protected Server server;
	
	public CommandParser(Server server, boolean log) {
		this.server = server;
	}
	
	public abstract boolean match(String answer);
	public abstract void parseCommand(String answer) throws ServerException;
}
