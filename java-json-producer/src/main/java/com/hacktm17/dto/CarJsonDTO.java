package com.hacktm17.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Created by darkg on 27-May-17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarJsonDTO {

    @JsonProperty
    private Long timestamp;

    @JsonProperty
    private Double speed;

    @JsonProperty
    private Long recordingId;

    @JsonProperty
    private Double longitude;

    @JsonProperty
    private Double latitude;

    @JsonProperty
    private Double[] trafficHeading = new Double[]{};

    @JsonProperty
    private Double[] trafficXSpeed = new Double[]{};

    @JsonProperty
    private Double[] trafficYSpeed = new Double[]{};

    @JsonProperty
    private Double[] trafficLength = new Double[]{};

    @JsonProperty
    private Double[] trafficXPos = new Double[]{};

    @JsonProperty
    private Double[] trafficYPos = new Double[]{};

    @JsonProperty
    private Long driver;

    @JsonProperty
    private Double rpm;

    @JsonProperty
    private Double yawRate;

    @JsonProperty
    private Double pedal;
}
