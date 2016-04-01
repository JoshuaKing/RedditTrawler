package application;

import reddit.RedditRobot;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Reddit Trawler");
        new RedditRobot();
        //DynamoDb dynamoDb = new DynamoDb();
        //Table table = dynamoDb.createTable();
        System.out.println("Spawned.");
    }
}
