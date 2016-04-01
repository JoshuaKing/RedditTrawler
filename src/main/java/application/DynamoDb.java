package application;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.Arrays;

/**
 * Created by Josh on 1/04/2016.
 */
public class DynamoDb {
    private static final DynamoDB DYNAMO = new DynamoDB(new AmazonDynamoDBClient());
    private static final String TABLE_TOTAL_WORDS = "TotalWordIndex";


    public Table createTable() throws InterruptedException {
        Table table = DYNAMO.createTable(
                TABLE_TOTAL_WORDS,
                Arrays.asList(
                    new KeySchemaElement("word", KeyType.HASH),
                    new KeySchemaElement("count", KeyType.RANGE)
                ), Arrays.asList(
                    new AttributeDefinition("word", ScalarAttributeType.S),
                    new AttributeDefinition("count", ScalarAttributeType.N)
                ), new ProvisionedThroughput(5L, 5L)
        );
        table.waitForActive();
        System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());
        return table;
    }
}
