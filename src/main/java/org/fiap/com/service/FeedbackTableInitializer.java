package org.fiap.com.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@ApplicationScoped
public class FeedbackTableInitializer {

    private static final Logger LOGGER = Logger.getLogger(FeedbackTableInitializer.class);

    @Inject
    DynamoDbClient dynamoDb;

    void onStart(@Observes StartupEvent event) {
        if (tableExists()) {
            return;
        }

        LOGGER.infof("Creating DynamoDB table '%s'", AbstractFeedbackService.FEEDBACK_TABLE_NAME);
        dynamoDb.createTable(CreateTableRequest.builder()
                .tableName(AbstractFeedbackService.FEEDBACK_TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .attributeName(AbstractFeedbackService.FEEDBACK_ID_COL)
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(AbstractFeedbackService.FEEDBACK_ID_COL)
                        .attributeType("N")
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build());

        try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(dynamoDb).build()) {
            waiter.waitUntilTableExists(DescribeTableRequest.builder()
                    .tableName(AbstractFeedbackService.FEEDBACK_TABLE_NAME)
                    .build());
        }
    }

    private boolean tableExists() {
        try {
            dynamoDb.describeTable(DescribeTableRequest.builder()
                    .tableName(AbstractFeedbackService.FEEDBACK_TABLE_NAME)
                    .build());
            return true;
        } catch (ResourceNotFoundException ignored) {
            return false;
        }
    }
}

