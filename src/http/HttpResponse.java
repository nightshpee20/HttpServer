package http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

import main.HttpServer;
import util.StatusCodes;

public class HttpResponse {
	public static final String WEBROOT_PATH = "C:\\Users\\night\\Desktop\\Java Internship\\HttpServer\\WEBROOT";
	
	Socket clientSocket;
	private HttpTask task;
	private HttpRequest request;
	
	StatusCodes code;
	
	public HttpResponse(HttpTask task, Socket client) {
		this.task = task;
		clientSocket = client;
		request = new HttpRequest(task, client);
		StatusCodes code = task.server.returnCompressed == true ? sendCompressedResponse() : sendResponse();
	}
	
	private StatusCodes sendResponse() {
		File requestedFile = new File(task.serverPath);
	
		StatusCodes statusCode = StatusCodes.OK;
		if (!requestedFile.exists()) {
			statusCode = StatusCodes.NOT_FOUND;
			requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");
		} else if (requestedFile.isDirectory()) {
			requestedFile = new File(task.serverPath + "\\index.html");
			if (!requestedFile.exists()) {
				statusCode = StatusCodes.NOT_FOUND;
				requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");					
			}
		}
		
		try (FileInputStream fis = new FileInputStream(requestedFile)){
			task.body = fis.readAllBytes();
		} catch (IOException e) {
			System.out.println("Error: Could not read the requested file!");
			//TODO: 500 Internal Server Error
			return null;
		}
		
		request.gatherResponseHeaders(requestedFile);

		StringBuilder headers = new StringBuilder();
		task.responseHeaders.forEach((key, val) -> {
			String hdr = String.format("%s: %s\n", key, val);
			headers.append(hdr);
		});
		
		OutputStream clientOutput = null;
		try {
			clientOutput = clientSocket.getOutputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			clientOutput.write(task.protocol.getBytes());
			clientOutput.write(" ".getBytes());
			clientOutput.write(statusCode.getCode().getBytes());
			clientOutput.write("\n".getBytes());
			clientOutput.write(headers.toString().getBytes());
			clientOutput.write("\n".getBytes());
			clientOutput.write(task.body);
			clientOutput.flush();
		} catch (IOException e) {
			System.out.println("ERROR: Failed to send response!");
			System.out.println(e.getMessage());
			return null;
		} 
		
		return statusCode;
	}

	private StatusCodes sendCompressedResponse() {
		OutputStream clientOutput = null;
		try {
			clientOutput = clientSocket.getOutputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		GZIPOutputStream gzipOutput = null;
		try {
			gzipOutput = new GZIPOutputStream(clientOutput);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		StatusCodes statusCode = StatusCodes.OK;
		File requestedFile = new File(task.serverPath);
		
		if (requestedFile.isDirectory()) {
			requestedFile = new File(task.serverPath + "\\index.html.gz");
			
			if (!requestedFile.exists()) {
				statusCode = StatusCodes.NOT_FOUND;
				requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");					
			}
		} else if (!(requestedFile = new File(task.serverPath + ".gz")).exists()) {
			statusCode = StatusCodes.NOT_FOUND;
			requestedFile = new File(WEBROOT_PATH + "\\404_NOT_FOUND.jpg");
		}
		
		try (FileInputStream fis = new FileInputStream(requestedFile)){
			task.body = fis.readAllBytes();
		} catch (IOException e) {
			System.out.println("Error: Could not read the requested file!");
			//TODO: 500 Internal Server Error
			return null;
		}
		
		request.gatherResponseHeaders(requestedFile);
		task.responseHeaders.put("Content-Encoding", "gzip");
		
		StringBuilder headers = new StringBuilder();
		task.responseHeaders.forEach((key, val) -> {
			String hdr = String.format("%s: %s\n", key, val);
			headers.append(hdr);
		});
		
		System.out.println(requestedFile.getAbsolutePath());
		try {
			clientOutput.write(task.protocol.getBytes());
			clientOutput.write(" ".getBytes());
			clientOutput.write(statusCode.getCode().getBytes());
			clientOutput.write("\n".getBytes());
			clientOutput.write(headers.toString().getBytes());
			clientOutput.write("\n".getBytes());
			gzipOutput.write(task.body);
			clientOutput.flush();
			gzipOutput.flush();
		} catch (IOException e) {
			System.out.println("ERROR: Failed to send response!");
			System.out.println(e.getMessage());
			return null;
		} 
		
		return statusCode;
	}
}
