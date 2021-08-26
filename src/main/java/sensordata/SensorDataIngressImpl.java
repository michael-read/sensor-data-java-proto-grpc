package sensordata;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import cloudflow.akkastream.WritableSinkRef;

import java.util.concurrent.CompletionStage;

// tag::ingressImpl[]
public class SensorDataIngressImpl implements SensorDataService {
    private final WritableSinkRef<SensorData> sinkRef;

    public SensorDataIngressImpl(WritableSinkRef<SensorData> sinkRef) {
        this.sinkRef = sinkRef;
    }

    private CompletionStage<SensorDataReply> provideMethod(SensorData in) {
        return sinkRef.writeJava(in)
                .thenApply(result -> SensorDataReply.newBuilder()
                        .setDeviceId(in.getDeviceId())
                        .setSuccess(true)
                        .build())
                .exceptionally(e -> SensorDataReply.newBuilder()
                        .setDeviceId(in.getDeviceId())
                        .setSuccess(false)
                        .setFailureMsg(e.getMessage())
                        .build());
    }

    @Override
    public CompletionStage<SensorDataReply> provide(SensorData in) {
        return provideMethod(in);
    }

    @Override
    public Source<SensorDataReply, NotUsed> provideStreamed(Source<SensorData, NotUsed> in) {
        return in.mapAsync(5, this::provideMethod);
    }

}
// end::ingressImpl[]