package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class HttpRequest {
	private static Map<String, String> contentTypes;
	private HttpTask task;
	private Socket clientSocket;
	
	public HttpRequest(HttpTask task, Socket client) {
		this.task = task;
		clientSocket = client;
		fillContentTypes();
	}
	
	public void gatherResponseHeaders(File requestedFile) {
		//Content-Type
		String fileName = requestedFile.getName();
		int dotIndex = fileName.indexOf('.');
		String format = fileName.substring(dotIndex + 1, fileName.length());
		task.responseHeaders.put("Content-Type", contentTypes.get(format));
		
		//Content-Length
		task.responseHeaders.put("Content-Length", String.valueOf(task.body.length));
		
		//Date
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, d MMM u H:m:s ");
		ZoneId GMT = TimeZone.getDefault().toZoneId();

		String date = LocalDateTime.now().format(formatter);
		task.responseHeaders.put("Date", date);
		
		//Last-Modified
		long lmLong = requestedFile.lastModified();
		Instant lmInstant = Instant.ofEpochMilli(lmLong);
		
		String lmStr = LocalDateTime.ofInstant(lmInstant, GMT).format(formatter) + "GMT"; 
		task.responseHeaders.put("Last-Modified", lmStr);
	}

	public void extractDetails() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		String line = br.readLine();
		
		if (line != null && !line.isBlank()) {
			String[] lineParts = line.split(" ");
			
			if (!lineParts[2].startsWith("HTTP"))
				throw new IOException("Protocol not supported: " + lineParts[2]);
			if (!lineParts[0].equals("GET"))
				throw new IOException("Method not supported: " + lineParts[2]);
			
			Path pathP = null;
			try {
				pathP = Path.of(HttpResponse.WEBROOT_PATH, lineParts[1]);					
			} catch (InvalidPathException e) {
				throw new IOException("Invalid path: " + lineParts[1]);
			}
			
			
			task.method = lineParts[0];
			task.path = lineParts[1];
			task.protocol = lineParts[2];
			task.serverPath = pathP.toString();
		}
		
		line = br.readLine();
		
		while (line != null && !line.isBlank()) {
			int firstColonIndex = line.indexOf(':');
			
			String key = line.substring(0, firstColonIndex);
			String value = line.substring(firstColonIndex + 2, line.length());
			
			task.requestHeaders.put(key, value);
			
			line = br.readLine();
		}
		
		br.close();
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
}
