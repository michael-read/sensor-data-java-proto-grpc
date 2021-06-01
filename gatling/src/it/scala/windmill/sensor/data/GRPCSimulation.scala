package windmill.sensor.data

import com.github.phisgr.gatling.grpc.Predef.{$, grpc, managedChannelBuilder}
import com.github.phisgr.gatling.pb.{EpxrLens, value2ExprUpdatable}
import io.gatling.core.Predef._
import io.gatling.core.Predef.{stringToExpression => _, _}
import io.gatling.core.session.Expression
import sensordata.Measurements.Measurements
import sensordata.SensorData.{SensorData, SensorDataServiceGrpc}

import java.time.Instant
import scala.concurrent.duration._

class GRPCSimulation extends Simulation {

  val measurementsFeeder = csv("measurements.csv").random
  val devideIds = csv("deviceids.csv").random

  val timestamps = Iterator.continually(
    Map("timestamp" -> Instant.now().getEpochSecond)
  )

  val grpcConf = grpc(managedChannelBuilder("localhost", 8080).usePlaintext())

  val scn = scenario("Scenario Name") // A scenario is a chain of requests and pauses
    .feed(timestamps)
    .feed(devideIds)
    .feed(measurementsFeeder)
    .exec(
      grpc("request_1")
        .rpc(SensorDataServiceGrpc.METHOD_PROVIDE)
        .payload(ses =>
          for (
            deviceId <- ses("deviceid").validate[String];
            timestamp <- ses("timestamp").validate[Long];
            power <- ses("power").validate[Double];
            rotorSpeed <- ses("rotorSpeed").validate[Double];
            windSpeed <- ses("windSpeed").validate[Double]
          ) yield SensorData(deviceId, timestamp, Some(Measurements(power, rotorSpeed, windSpeed)))
        )
    )

  setUp(
    // scn.inject(atOnceUsers(1000))
    //    scn.inject(atOnceUsers(1))
    //    scn.inject(rampUsers(100) during (3 minutes))
    // scn.inject(rampUsers(100) during (5 minutes))
    // simulation set up -> https://gatling.io/docs/current/general/simulation_setup/
        scn.inject(
          nothingFor(4 seconds), // 1
          atOnceUsers(10), // 2
          rampUsers(10) during (5 seconds), // 3
          constantUsersPerSec(20) during (15 seconds), // 4
          constantUsersPerSec(20) during (15 seconds) randomized, // 5
          rampUsersPerSec(100) to 20 during (10 minutes), // 6
          rampUsersPerSec(100) to 20 during (10 minutes) randomized, // 7
          heavisideUsers(2000) during (20 seconds), // 8
          heavisideUsers(4000) during (40 seconds), // 8
          heavisideUsers(10000) during (30 seconds) // 8
        )
    //    scn.inject(atOnceUsers(1))
      //    scn.inject(rampUsers(100) during (3 minutes))

  )
  .protocols(grpcConf)
}
