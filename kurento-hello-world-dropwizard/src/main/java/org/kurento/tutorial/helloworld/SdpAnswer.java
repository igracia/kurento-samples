
package org.kurento.tutorial.helloworld;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SdpAnswer {
  private String sdpAnswer;

  public SdpAnswer() {
    // Jackson deserialization
  }

  public SdpAnswer(String sdpAnswer) {
    this.sdpAnswer = sdpAnswer;
  }

  @JsonProperty
  public String getSdpAnswer() {
    return sdpAnswer;
  }

}
