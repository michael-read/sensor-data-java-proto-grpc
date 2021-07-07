package client.sensordata;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import akka.stream.javadsl.Source;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SensorDataClientStream {

    void doWork(String[] args) {
        ActorSystem system = ActorSystem.create("SensorDataClient");
        Materializer materializer = SystemMaterializer.get(system).materializer();
        GrpcClientSettings settings = GrpcClientSettings.fromConfig("client.SensorDataService", system);
        SensorDataServiceClient client = null;
        
        List<String> samples = new ArrayList<>();

        try {
            client = SensorDataServiceClient.create(settings, system);

            List<String> filenames = (args.length == 0) ? Collections.singletonList("../test-data/one-record-per-line.json") : Arrays.asList(args);
            for (String filename : filenames) {
                File file = new File(filename);
                FileReader fr = new FileReader(file);   //reads the file
                BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
                String line;
                while( ( line = br.readLine()) !=null ) {
                    samples.add(line);
                }
                fr.close();    //closes the stream and release the resources
            }

            AtomicInteger sent = new AtomicInteger();
            Source<SensorData, NotUsed> requestStream =
                Source.fromIterator(samples::iterator)
                  .map(sample -> {
                        SensorData.Builder sensorBuilder = SensorData.newBuilder();
                        JsonFormat.parser().merge(sample, sensorBuilder);
                        SensorData data = sensorBuilder.build();
                        sent.getAndIncrement();
                        System.out.printf("transmitting data for device Id (%d): %s%n", sent.get(), data.getDeviceId());
                        return data;
                });

            Source<SensorDataReply, NotUsed> responseStream = client.provideStreamed(requestStream);

            AtomicInteger received = new AtomicInteger();
            CompletionStage<Done> done =
                    responseStream.runForeach((reply) -> {
                        received.getAndIncrement();
                        System.out.printf("received streaming reply: for device ID (%d): %s%n", received.get(), reply.getDeviceId());
                    }, materializer);

            done.toCompletableFuture().get(60, TimeUnit.SECONDS);
            System.out.printf("requests sent %d, replies received %d%n", sent.get(), received.get());

        } catch (StatusRuntimeException e) {
            System.out.println("Status: " + e.getStatus());
        } catch (Exception e)  {
            e.printStackTrace();
        } finally {
            if (client != null) client.close();
            System.out.println("finally done.");
            system.terminate();
        }
    }

    public static void main(String[] args) {
        new SensorDataClientStream().doWork(args);
    }

}