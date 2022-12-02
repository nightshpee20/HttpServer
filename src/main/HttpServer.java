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

import http.HttpTask;
import util.ServerOptions;
import util.StatusCodes;

public class HttpServer {
	public static final int DEFAULT_THREADS = 1;
	public static final int DEFAULT_PORT = 80;
	public static final int MAX_THREADS = 10;
	public static final int MAX_PORT = 65_535;
	
	public static int threadsCount;
	public static int port;
	public static boolean returnDirContents;
	public static boolean returnGzip;
	public static boolean returnCompressed;
	
	public static Options options = new Options();
	
	public static ExecutorService threadPool;
	
	
	
	public HttpServer(String[] args) {
		if (ServerOptions.createAndParseServerOptions(this, args))
			return;
		threadPool = Executors.newFixedThreadPool(threadsCount);
	}

	public void start() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server start!");
			
			while (true) {
				Socket client = serverSocket.accept();
				HttpTask task = new HttpTask(client, this);
				threadPool.execute(task);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
