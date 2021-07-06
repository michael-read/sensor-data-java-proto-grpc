package client.sensordata;

import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import com.google.protobuf.util.JsonFormat;
import io.grpc.StatusRuntimeException;
import sensordata.SensorData;
import sensordata.SensorDataReply;
import sensordata.SensorDataServiceClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

class SensorDataClientForEach {

    public <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFuturesResult =
                CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        return allFuturesResult.thenApply(v ->
                futuresList.stream().
                        map(future -> future.join()).
                        collect(Collectors.<T>toList())
        );
    }

    CompletableFuture<SensorDataReply> singleRequestReply(SensorDataServiceClient client, SensorData data, int cnt) {
        System.out.println(String.format("transmitting data for device Id (%d): %s", cnt, data.getDeviceId()));
        CompletionStage<SensorDataReply> reply = client.provide(data);
        reply.thenAccept(msg -> {
            System.out.println(String.format("received response for device Id (%d): %s", cnt, msg.getDeviceId()));
        })
        .exceptionally(ex -> {
            System.out.println(String.format("Something went wrong, Error (%d): %s", cnt, ex.getMessage()));
            return null;
        });
        return (CompletableFuture<SensorDataReply>) reply;
    }

    void doWork(String[] args) {
        ActorSystem system = ActorSystem.create("SensorDataClient");
        GrpcClientSettings settings = GrpcClientSettings.fromConfig("client.SensorDataService", system);
        SensorDataServiceClient client = null;
        List<CompletableFuture<SensorDataReply>> replies = new ArrayList<>();

        try {
            client = SensorDataServiceClient.create(settings, system);

            List<String> filenames = (args.length == 0) ? Arrays.asList("../test-data/one-record-per-line.json") : Arrays.asList(args);
            int cnt = 0;

            for (String filename : filenames) {
                File file = new File(filename);
                FileReader fr = new FileReader(file);   //reads the file
                BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
                String line;
                while( ( line = br.readLine()) !=null ) {
                    SensorData.Builder sensorBuilder = SensorData.newBuilder();
                    JsonFormat.parser().merge(line, sensorBuilder);
                    SensorData data = sensorBuilder.build();
                    cnt++;
                    replies.add(singleRequestReply(client, data, cnt));
                }
                fr.close();    //closes the stream and release the resources
            }
            System.out.println(String.format("requests sent %d", replies.size()));

        } catch (StatusRuntimeException e) {
            System.out.println("Status: " + e.getStatus());
        } catch (Exception e)  {
            e.printStackTrace();
        } finally {
            allOf(replies).join();
            if (client != null) client.close();
            System.out.println("finally done.");
            system.terminate();
        }
    }

    public static void main(String[] args) throws Exception {
        new SensorDataClientForEach().doWork(args);
    }

}