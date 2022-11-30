package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
	public static void main(String[] args) {
		File file = new File("C:\\Users\\night\\Desktop\\Java Internship\\HttpServer\\WEBROOT\\403_FORBIDDEN.jpg");
		System.out.println(file.getName());
		
		long lmLong = file.lastModified();
		Instant lmInstant = Instant.ofEpochMilli(lmLong);
		LocalDateTime date = LocalDateTime.now();
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, d MMM u H:m:s ");
		ZoneId GMT = TimeZone.getDefault().toZoneId();
		String lmStr = LocalDateTime.ofInstant(lmInstant, GMT).format(formatter) + "GMT";  

		System.out.println(lmStr);
	}
}
//	private static final int MAX_PORT = 32_767;
//	private static final String WEBROOT_PATH = "C:\\Users\\night\\Desktop\\Java Internship\\HttpServer\\WEBROOT";
//	
//	private static int threadsCount = 1;
//	private static int port = 80;
//	private static boolean returnDirContents = false;
//
//	private static Options options = new Options();
//	
//	private static String requestMethod;
//	private static String requestPath;
//	private static String requestProtocol;
//	
//	public static void main(String[] args) {
//		setOptions();
//		if (!parseOptions(args))
//			return;
//		
//		//////////////////////////////////////////////
//
//		try (ServerSocket serverSocket = new ServerSocket(3467)) {
//			System.out.println("Server start!");
//			
//			while (true) {
//				try (Socket client = serverSocket.accept()) {				
//					InputStreamReader clientInput = new InputStreamReader(client.getInputStream());
//					BufferedReader br = new BufferedReader(clientInput);
//					StringBuilder sb = new StringBuilder();
//					
//					String line = br.readLine();
//					if (line != null && !line.isBlank()) {
//						sb.append(line + "\n");
//					} else {
//						//TODO: Return invalid request response
//						System.out.println("Invalid request!");
//						return;
//					}
//					
//					try {
//						getMethodPathAndProtocol(line);
//					} catch (HttpServerException e) {
//						System.out.println(e.getMessage());
//						return;
//					}
//					
//					System.out.println("==REQUEST==");
//					System.out.println(sb);
//					System.out.println(requestPath);
//					
//					String statusCode = "200 OK\n";
//					
//					File requestedFile = new File(requestPath);
//					if (!requestedFile.exists())
//						statusCode = "404 NOT FOUND";
//					
//					OutputStream clientOutput = client.getOutputStream();
//					clientOutput.write(requestProtocol.getBytes());
//					clientOutput.write(" ".getBytes());
//					clientOutput.write(statusCode.getBytes());
//					clientOutput.write(("\n").getBytes());
//					
//					if (statusCode.startsWith("404")) 
//						requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");
//					
//					if (requestedFile.isDirectory() && returnDirContents) {
//						StringBuilder dirSb = new StringBuilder();
//						
//						Stream<Path> dirContents = Files.walk(requestedFile.toPath());
//						
//						dirContents.forEach((path) -> {
//							String htmlEl = String.format("<h6>%s<h6>\n", path.getFileName());
//							dirSb.append(htmlEl);
//						});
//						
//						clientOutput.write(sb.toString().getBytes());
//					} else if (requestedFile.isDirectory()) {
//						requestedFile = new File(WEBROOT_PATH + "\\403_FORBIDDEN.jpg");
//						FileInputStream fis = new FileInputStream(requestedFile);
//						clientOutput.write(fis.readAllBytes());
//					} else {						
//						FileInputStream fis = new FileInputStream(requestedFile);
//						clientOutput.write(fis.readAllBytes());						
//					}
//					
//					
//					clientOutput.flush();
//					clientInput.close();
//					clientOutput.close();
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		//////////////////////////////////////////////
//	}
//
//	private static void getMethodPathAndProtocol(String line) throws HttpServerException {
//		String[] lineParts = line.split(" ");
//		
//		String protocol = lineParts[2].strip();
//		if (!protocol.startsWith("HTTP"))
//			throw new HttpServerException("This protocol is not supported: " + protocol);
//		requestProtocol = protocol;
//		
//		String method = lineParts[0].strip();
//		if (!method.equals("GET"))
//			throw new HttpServerException("This method is not supported: " + method);
//		requestMethod = method;
//		
//		String pathStr = lineParts[1].strip();
//		Path path = null;
//		try {
//			path = Path.of(pathStr);
//		} catch (InvalidPathException e) {
//			throw new HttpServerException("This path does not exist: " + path);
//		}
//		
//		requestPath = WEBROOT_PATH + path.toString();
//		if (requestPath.equals("\\"))
//			requestPath += "index.html";
//	}
//	private static void setOptions() {
//		Option port = new Option("p", "port", true, "-p/--port [port number], sets up the port on which the server will listen for requests.");
//		Option threads = new Option("t", "threads", true, "-t/--threads [number of threads(max 10)], sets up the number of threads that will concurrently respond to requests.");
//		options.addOption(new Option("d", false, "-d, if the requested resource is a directory, the response will contain the directory's content."));
//		options.addOption(new Option("h", "help", false, "list all available options and their description."));
//		
//		options.addOption(port);
//		options.addOption(threads);
//		
//		port.setArgs(1);
//		threads.setArgs(1);
//	}
//	
//	private static boolean parseOptions(String[] args) {
//		CommandLineParser cmp = new DefaultParser();
//		
//		try {
//			CommandLine cm = cmp.parse(options, args);
//			
//			if (cm.hasOption("h")) {
//				HelpFormatter hf = new HelpFormatter();
//				
//				hf.printHelp("HttpServer Options:", options);
//			}
//			
//			if (cm.hasOption("p")) {
//				int newPort = -1;
//				try {
//					newPort = Integer.parseInt(cm.getOptionValue("p"));
//					if (newPort < 0 || MAX_PORT < newPort) {
//						System.out.println("Invalid -p/--port argument!");
//						return false;
//					}
//				} catch (NumberFormatException e) {
//					System.out.println("Invalid -p/--port argument!");
//					return false;
//				}
//				port = newPort;
//			}
//			
//			if (cm.hasOption("t")) {
//				int newThreadsCount = -1;
//				try {
//					newThreadsCount = Integer.parseInt(cm.getOptionValue("t"));
//					if (newThreadsCount < 1 || 10 < newThreadsCount) {
//						System.out.println("Invalid -t/--threads argument!");
//						return false;
//					}
//				} catch (NumberFormatException e) {
//					System.out.println("Invalid -t/--threads argument!");
//					return false;
//				}
//				threadsCount = newThreadsCount;
//			}
//			
//			if (cm.hasOption("d"))
//				returnDirContents = true;
//			
//		} catch (ParseException e) {
//			System.out.println("An error has occured while parsing the input options!\n " + e.getMessage());
//			return false;
//		}
//		
//		return true;
//	}
//
//}
//
//@SuppressWarnings("serial")
//class HttpServerException extends Throwable {
//	public HttpServerException(String errorMessage) {
//        super(errorMessage);
//    }
//}