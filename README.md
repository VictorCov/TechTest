
# TechTest
## GlobalMobilityApexTest

### Setting up the Environment
To set up the environment for this test, you need to run the `docker-compose.yml` file first.

1. Navigate to the directory where the `docker-compose.yml` file is located.
2. Run the following command:
   ```bash
   docker-compose up -d
   ```

### Sending a Message to a Kafka Topic
To send a message to a Kafka topic, follow these steps:

1. Create a text file with the following content (this represents an order event):
   ```json
   {"orderId":"order-009","customerId":"customer-001","products":[{"productId":"product-101"},{"productId":"product-1002"}]}
   ```
2. Save the file as `KafkaOrder.txt`.

3. Use the following command to send the message via console:
   ```bash
   kafka-console-producer --broker-list localhost:9092 --topic orders_topic < /Path/To/Your/File/KafkaOrder.txt
   ```

Make sure to replace `/Path/To/Your/File/` with the actual path to your `KafkaOrder.txt` file.
