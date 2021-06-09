# Cloudflow Set-up Notes

## Using a single Kafka replica in Cloudflow

helm install cloudflow cloudflow-helm-charts/cloudflow --namespace cloudflow \
  --set kafkaClusters.default.bootstrapServers=cloudflow-strimzi-kafka-bootstrap.cloudflow:9092 \
  --set kafkaClusters.default.replicas=1 --version "2.1.2"

helm upgrade cloudflow cloudflow-helm-charts/cloudflow --namespace cloudflow \
  --set kafkaClusters.default.bootstrapServers=cloudflow-strimzi-kafka-bootstrap.cloudflow:9092 \
  --set kafkaClusters.default.replicas=1 --version "2.1.2"

## Deployment 
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-java-proto-grpc.json -u <dockerhub-username> --password-stdin

### With Lightbend Telemetry
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-java-proto-grpc.json -u <dockerhub-username> --password-stdin --conf telemetry.conf

### With Lightbend Telemetry, turn down Kafka
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-java-proto-grpc.json -u <dockerhub-username> --password-stdin --conf telemetry.conf --logback-config logback.xml

k cloudflow configure sensor-data-java-proto-grpc --conf telemetry.conf --logback-config logback.xml
kubectl cloudflow configure sensor-data-scala-grpc --conf limit.conf

# follow logs of all pods
k -n sensor-data-java-proto-grpc logs -f -l com.lightbend.cloudflow/app-id=sensor-data-java-proto-grpc