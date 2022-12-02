package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import main.HttpServer;
import util.StatusCodes;

public class HttpTask implements Runnable {
	private Socket clientSocket;
	String method;
	String path;
	String protocol;
	Map<String, String> requestHeaders;
	Map<String, String> responseHeaders;
	byte[] body;

	String serverPath;
	HttpServer server;
	
	public HttpTask(Socket client, HttpServer server) throws IOException {
		clientSocket = client;
		requestHeaders = new HashMap<>();
		responseHeaders = new HashMap<>();
		this.server = server;
	}
	
	@Override
	public void run() {
		HttpResponse response = new HttpResponse(this, clientSocket);
		
		try {
			clientSocket.close();
		} catch (IOException e) {
			System.out.println("ERROR: Failed to close socket");
			return;
		}
		
		String userAgent = null;
		if (response.code == StatusCodes.OK) 
			userAgent = String.format("\"%s\"", requestHeaders.get("User-Agent"));
		else 	
			userAgent = String.format("\"Error: %s\"", response.code.getCode());
		
		String logMsg = String.format("[%s] \"%s %s\" %s", 
				LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), method, path, userAgent);
		System.out.println(logMsg);
	}
}
