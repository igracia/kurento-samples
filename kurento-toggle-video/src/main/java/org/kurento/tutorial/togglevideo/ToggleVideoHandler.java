/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.togglevideo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Hello World handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0
 */
public class ToggleVideoHandler extends TextWebSocketHandler {

  private static final Gson gson = new GsonBuilder().create();
  private final Logger log = LoggerFactory.getLogger(ToggleVideoHandler.class);

  @Autowired
  private KurentoClient kurento;

  private final Map<String, UserSession> users = new ConcurrentHashMap<>();

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "processClientOffer":
        processClientOffer(session, jsonMessage);
        break;
      case "stop": {
        UserSession user = users.remove(session.getId());
        if (user != null) {
          user.release();
        }
        break;
      }
      case "onIceCandidate": {
        JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

        UserSession user = users.get(session.getId());
        if (user != null) {
          IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
              jsonCandidate.get("sdpMid").getAsString(),
              jsonCandidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(candidate);
        }
        break;
      }
      default:
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

  private void processClientOffer(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      UserSession user = users.get(session.getId());
      if (user == null) {
        log.info("Creating new user session for user");
        MediaPipeline pipeline = kurento.createMediaPipeline();
        user = new UserSession();
        user.setMediaPipeline(pipeline);
        users.put(session.getId(), user);
      }

      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(user.getMediaPipeline()).build();
      webRtcEndpoint.connect(webRtcEndpoint);
      user.setWebRtcEndpoint(webRtcEndpoint);
      // 3. SDP negotiation
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = user.getWebRtcEndpoint().processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "processClientOfferResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      // 4. Gather ICE candidates
      user.getWebRtcEndpoint().addIceCandidateFoundListener(event -> {
        JsonObject candidateMsg = new JsonObject();
        candidateMsg.addProperty("id", "iceCandidate");
        candidateMsg.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
        try {
          synchronized (session) {
            session.sendMessage(new TextMessage(candidateMsg.toString()));
          }
        } catch (IOException e) {
          log.error(e.getMessage());
        }
      });

      user.getWebRtcEndpoint().gatherCandidates();

    } catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }

  private void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }
}
