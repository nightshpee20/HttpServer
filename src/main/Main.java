package main;

public class Main {
	public static void main(String[] args) {
		HttpServer server = new HttpServer(args);
		server.start();
	}
}