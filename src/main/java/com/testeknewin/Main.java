package main.java.com.testeknewin;

import main.java.com.testeknewin.controller.Crawler;

public class Main {
    public static void main(String[] args) {
        Crawler crawler = new Crawler(3);
        crawler.start();
    }
}