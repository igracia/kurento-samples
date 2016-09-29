
package org.kurento.tutorial.helloworld;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

  private final Logger log = LoggerFactory.getLogger(HelloWorldResource.class);

  private final KurentoClient kurento;

  private final Map<String, UserSession> users = new ConcurrentHashMap<>();

  public HelloWorldResource(KurentoClient kruentoClient) {
    this.kurento = kruentoClient;
  }

  @POST
  @Path("/start/{id}")
  @Timed
  public SdpAnswer start(@PathParam("id") String userId, String sdpOffer) {

    MediaPipeline pipeline = kurento.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    // 2. Store user session
    UserSession user = new UserSession();
    user.setMediaPipeline(pipeline);
    user.setWebRtcEndpoint(webRtcEndpoint);
    users.put(userId, user);

    String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

    final CountDownLatch latch = new CountDownLatch(1);

    webRtcEndpoint.addOnIceGatheringDoneListener(event -> {
      latch.countDown();
    });

    webRtcEndpoint.gatherCandidates();
    try {
      latch.await();
    } catch (InterruptedException e) {
      // Should not reach here
    }

    sdpAnswer = webRtcEndpoint.getLocalSessionDescriptor();

    return new SdpAnswer(sdpAnswer);
  }

  @POST
  @Path("/stop/{id}")
  @Timed
  public void stop(@PathParam("id") String userId) {
    UserSession user = users.remove(userId);
    if (user != null) {
      user.release();
    }

  }

  @POST
  @Path("/addIceCandidate/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void addIceCandidate(@PathParam("id") String userId, IceCandidate candidate) {

    UserSession user = users.get(userId);
    if (user != null) {
      user.addCandidate(candidate);
    }

  }

}