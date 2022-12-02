package util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import main.HttpServer;

public class ServerOptions {
	private static HttpServer server;
	
	public static boolean createAndParseServerOptions(HttpServer _server, String[] args) {
		server = _server;
		setOptions();
		return parseOptions(args);
	}
	
	private static void setOptions() {
		Options options = server.options;
		Option port = new Option("p", "port", true, "-p/--port [port number], sets up the port on which the server will listen for requests.");
		Option threads = new Option("t", "threads", true, "-t/--threads [number of threads(max 10)], sets up the number of threads that will concurrently respond to requests.");
		options.addOption(new Option("d", false, "-d, if the requested resource is a directory, the response will contain the directory's content."));
		options.addOption(new Option("h", "help", false, "list all available options and their description."));
		options.addOption(new Option("c", "compress", false, "creates compressed versions of text files"));
		options.addOption(new Option("g", "gzip", false, "returns the compressed version of the requested file"));
		
		options.addOption(port);
		options.addOption(threads);
		
		port.setArgs(1);
		threads.setArgs(1);
	}
	
	private static boolean parseOptions(String[] args) {
		CommandLineParser cmp = new DefaultParser();
		
		try {
			CommandLine cm = cmp.parse(server.options, args);
			
			if (cm.hasOption("h")) {
				HelpFormatter hf = new HelpFormatter();
				
				hf.printHelp("HttpServer Options:", server.options);
			}
			
			if (cm.hasOption("p")) {
				int newPort = -1;
				try {
					newPort = Integer.parseInt(cm.getOptionValue("p"));
					if (newPort < 0 || server.MAX_PORT < newPort) {
						System.out.println("Invalid -p/--port argument!");
						return false;
					}
				} catch (NumberFormatException e) {
					System.out.println("Invalid -p/--port argument!");
					return false;
				}
				server.port = newPort;
			} else
				server.port = server.DEFAULT_PORT;
			
			if (cm.hasOption("t")) {
				int newThreadsCount = -1;
				try {
					newThreadsCount = Integer.parseInt(cm.getOptionValue("t"));
					if (newThreadsCount < server.DEFAULT_THREADS || server.MAX_THREADS < newThreadsCount) {
						System.out.println("Invalid -t/--threads argument!");
						return false;
					}
				} catch (NumberFormatException e) {
					System.out.println("Invalid -t/--threads argument!");
					return false;
				}
				server.threadsCount = newThreadsCount;
			} else
				server.threadsCount = server.DEFAULT_THREADS;
			
			server.returnDirContents = cm.hasOption("d") ? true : false;
			server.returnGzip = cm.hasOption("g") ? true : false;
			server.returnCompressed = cm.hasOption("c") ? true : false;
		} catch (ParseException e) {
			System.out.println("An error has occured while parsing the input options!\n " + e.getMessage());
			return false;
		}
			
		return true;
	}
}
