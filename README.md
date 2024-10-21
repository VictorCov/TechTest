# TechTest
GlobalMobilityApexTest

*In order to setup the right environment for this test, please run the docker-compose.yml file first.
  *in the docker-compose-yml file location, run: docker-compose up -d

To send a message to a Kakfka topic you can do the following:
*Create a txt file with the following content (this is the order event):
  {"orderId":"order-009","customerId":"customer-001","products":[{"productId":"product-101"},{"productId":"product-1002"}]}
*I named it KafkaOrder.txt

*You can send the message by console by using the following command:
  kafka-console-producer --broker-list localhost:9092 --topic orders_topic < /Path/To/Your/File/KafkaOrder.txt
