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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class RefactoredMain {
	static class HttpTask implements Runnable {
		Socket clientSocket;
		InputStream clientInput;
		OutputStream clientOutput;
		InputStreamReader isr;
		BufferedReader br;
		
		String method;
		String path;
		String protocol;
		Map<String, String> requestHeaders;
		Map<String, String> responseHeaders;
		byte[] body;
	
		String serverPath;
		
		HttpTask(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;
			clientInput = clientSocket.getInputStream();
			isr = new InputStreamReader(clientInput);
			br = new BufferedReader(isr);
			clientOutput = clientSocket.getOutputStream();
			requestHeaders = new HashMap<>();
			responseHeaders = new HashMap<>();
		}
		
		@Override
		public void run() {
			try {
				extractDetails();
			} catch (IOException e) {
				System.out.println("ERROR: Could not extract request details!");
				return;
			}
			
			StatusCodes code = returnCompressed == true ? sendCompressedResponse() : sendResponse();
		
			try {
				clientInput.close();
				clientOutput.close();				
				isr.close();
				br.close();
				clientSocket.close();
			} catch (IOException e) {
				System.out.println("ERROR: Failed to close stream/reader/socket");
				return;
			}
			
			String userAgent = null;
			if (code == StatusCodes.OK) 
				userAgent = String.format("\"%s\"", requestHeaders.get("User-Agent"));
			else 	
				userAgent = String.format("\"Error: %s\"", code.getCode());
			
			String logMsg = String.format("[%s] \"%s %s\" %s", 
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), method, path, userAgent);
			System.out.println(logMsg);
		}
		
		private StatusCodes sendResponse() {
			File requestedFile = new File(serverPath);
		
			StatusCodes statusCode = StatusCodes.OK;
			if (!requestedFile.exists()) {
				statusCode = StatusCodes.NOT_FOUND;
				requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");
			} else if (requestedFile.isDirectory()) {
				requestedFile = new File(serverPath + "\\index.html");
				if (!requestedFile.exists()) {
					statusCode = StatusCodes.NOT_FOUND;
					requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");					
				}
			}
			
			try (FileInputStream fis = new FileInputStream(requestedFile)){
				body = fis.readAllBytes();
			} catch (IOException e) {
				System.out.println("Error: Could not read the requested file!");
				//TODO: 500 Internal Server Error
				return null;
			}
			
			gatherResponseHeaders(requestedFile);

			StringBuilder headers = new StringBuilder();
			responseHeaders.forEach((key, val) -> {
				String hdr = String.format("%s: %s\n", key, val);
				headers.append(hdr);
			});
			
			System.out.println(requestedFile.getAbsolutePath());
			try {
				clientOutput.write(protocol.getBytes());
				clientOutput.write(" ".getBytes());
				clientOutput.write(statusCode.getCode().getBytes());
				clientOutput.write("\n".getBytes());
				clientOutput.write(headers.toString().getBytes());
				clientOutput.write("\n".getBytes());
				clientOutput.write(body);
				clientOutput.flush();
			} catch (IOException e) {
				System.out.println("ERROR: Failed to send response!");
				System.out.println(e.getMessage());
				return null;
			} 
			
			return statusCode;
		}

		private StatusCodes sendCompressedResponse() {
			GZIPOutputStream gzipOutput = null;
			try {
				gzipOutput = new GZIPOutputStream(clientOutput);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			StatusCodes statusCode = StatusCodes.OK;
			File requestedFile = new File(serverPath);
			
			if (requestedFile.isDirectory()) {
				requestedFile = new File(serverPath + "\\index.html.gz");
				
				if (!requestedFile.exists()) {
					statusCode = StatusCodes.NOT_FOUND;
					requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");					
				}
			} else if (!(requestedFile = new File(serverPath + ".gz")).exists()) {
				statusCode = StatusCodes.NOT_FOUND;
				requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");
			}
			
			try (FileInputStream fis = new FileInputStream(requestedFile)){
				body = fis.readAllBytes();
			} catch (IOException e) {
				System.out.println("Error: Could not read the requested file!");
				//TODO: 500 Internal Server Error
				return null;
			}
			
			gatherResponseHeaders(requestedFile);
			responseHeaders.put("Content-Encoding", "gzip");
			
			StringBuilder headers = new StringBuilder();
			responseHeaders.forEach((key, val) -> {
				String hdr = String.format("%s: %s\n", key, val);
				headers.append(hdr);
			});
			
			System.out.println(requestedFile.getAbsolutePath());
			try {
				clientOutput.write(protocol.getBytes());
				clientOutput.write(" ".getBytes());
				clientOutput.write(statusCode.getCode().getBytes());
				clientOutput.write("\n".getBytes());
				clientOutput.write(headers.toString().getBytes());
				clientOutput.write("\n".getBytes());
				gzipOutput.write(body);
				clientOutput.flush();
				gzipOutput.flush();
			} catch (IOException e) {
				System.out.println("ERROR: Failed to send response!");
				System.out.println(e.getMessage());
				return null;
			} 
			
			return statusCode;
		}
		
		private void gatherResponseHeaders(File requestedFile) {
			//Content-Type
			String fileName = requestedFile.getName();
			int dotIndex = fileName.indexOf('.');
			String format = fileName.substring(dotIndex + 1, fileName.length());
			responseHeaders.put("Content-Type", contentTypes.get(format));
			
			//Content-Length
			responseHeaders.put("Content-Length", String.valueOf(body.length));
			
			//Date
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, d MMM u H:m:s ");
			ZoneId GMT = TimeZone.getDefault().toZoneId();

			String date = LocalDateTime.now().format(formatter);
			responseHeaders.put("Date", date);
			
			//Last-Modified
			long lmLong = requestedFile.lastModified();
			Instant lmInstant = Instant.ofEpochMilli(lmLong);
			
			String lmStr = LocalDateTime.ofInstant(lmInstant, GMT).format(formatter) + "GMT"; 
			responseHeaders.put("Last-Modified", lmStr);
		}

		private void extractDetails() throws IOException {
			String line = br.readLine();
			
			if (line != null && !line.isBlank()) {
				String[] lineParts = line.split(" ");
				
				if (!lineParts[2].startsWith("HTTP"))
					throw new IOException("Protocol not supported: " + lineParts[2]);
				if (!lineParts[0].equals("GET"))
					throw new IOException("Method not supported: " + lineParts[2]);
				
				Path pathP = null;
				try {
					pathP = Path.of(WEBROOT_PATH, lineParts[1]);					
				} catch (InvalidPathException e) {
					throw new IOException("Invalid path: " + lineParts[1]);
				}
				
				
				method = lineParts[0];
				path = lineParts[1];
				protocol = lineParts[2];
				serverPath = pathP.toString();
			}
			
			line = br.readLine();
			
			while (line != null && !line.isBlank()) {
				int firstColonIndex = line.indexOf(':');
				
				String key = line.substring(0, firstColonIndex);
				String value = line.substring(firstColonIndex + 2, line.length());
				
				requestHeaders.put(key, value);
				
				line = br.readLine();
			}
		}
	}
	
	private static final String WEBROOT_PATH = "C:\\Users\\night\\Desktop\\Java Internship\\HttpServer\\WEBROOT";
	private static final int DEFAULT_THREADS = 1;
	private static final int DEFAULT_PORT = 80;
	private static final int MAX_THREADS = 10;
	private static final int MAX_PORT = 65_535;
	
	private static int threadsCount;
	private static int port;
	private static boolean returnDirContents;
	private static boolean returnGzip;
	private static boolean returnCompressed;
	
	private static Options options = new Options();
	
	private static ExecutorService threadPool;
	
	private static enum StatusCodes {
		NOT_FOUND("404 NOT FOUND"), 
	    OK("200 OK"), 
	    FORBIDDEN("403 FORBIDDEN");
	 
		private String code;

		StatusCodes(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}
	
	private static Map<String, String> contentTypes;
	
	public static void main(String[] args) {
		setOptions();
		if (!parseOptions(args))
			return;
		
		contentTypes = fillContentTypes();
		threadPool = Executors.newFixedThreadPool(threadsCount);
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server start!");
			
			while (true) {
				Socket client = serverSocket.accept();
				HttpTask task = new HttpTask(client);
				threadPool.execute(task);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
		} catch (IOException e) {
			//TODO: ADD FUNCTIONALITY
			e.printStackTrace();
		}
	}
	
	private static Map<String, String> fillContentTypes() {
		Map<String, String> res = new HashMap<>();
		res.put("json","application/json");
		res.put("css","text/css");
		res.put("csv","text/csv");
		res.put("html","text/html");
		res.put("txt","text/plain");
		res.put("gif","image/gif");
		res.put("ico","image/vnd.microsoft.icon");
		res.put("jpg","image/jpeg");
		res.put("jpeg","image/jpeg");
		res.put("png","image/png");
		return res;
	}

	//DONE
	private static void setOptions() {
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
			CommandLine cm = cmp.parse(options, args);
			
			if (cm.hasOption("h")) {
				HelpFormatter hf = new HelpFormatter();
				
				hf.printHelp("HttpServer Options:", options);
			}
			
			if (cm.hasOption("p")) {
				int newPort = -1;
				try {
					newPort = Integer.parseInt(cm.getOptionValue("p"));
					if (newPort < 0 || MAX_PORT < newPort) {
						System.out.println("Invalid -p/--port argument!");
						return false;
					}
				} catch (NumberFormatException e) {
					System.out.println("Invalid -p/--port argument!");
					return false;
				}
				port = newPort;
			} else
				port = DEFAULT_PORT;
			
			if (cm.hasOption("t")) {
				int newThreadsCount = -1;
				try {
					newThreadsCount = Integer.parseInt(cm.getOptionValue("t"));
					if (newThreadsCount < DEFAULT_THREADS || MAX_THREADS < newThreadsCount) {
						System.out.println("Invalid -t/--threads argument!");
						return false;
					}
				} catch (NumberFormatException e) {
					System.out.println("Invalid -t/--threads argument!");
					return false;
				}
				threadsCount = newThreadsCount;
			} else
				threadsCount = DEFAULT_THREADS;
			
			returnDirContents = cm.hasOption("d") ? true : false;
			returnGzip = cm.hasOption("g") ? true : false;
			returnCompressed = cm.hasOption("c") ? true : false;
		} catch (ParseException e) {
			System.out.println("An error has occured while parsing the input options!\n " + e.getMessage());
			return false;
		}
			
		return true;
	}
}
