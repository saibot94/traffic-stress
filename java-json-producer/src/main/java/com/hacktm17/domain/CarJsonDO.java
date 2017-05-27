package com.hacktm17.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by darkg on 27-May-17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarJsonDO {

    @JsonProperty
    private Double speed;

    @JsonProperty
    private Double pedal;

    @JsonProperty
    private Double rpm;

    @JsonProperty
    private Double yawrate;

    @JsonProperty
    private Double latitude;

    @JsonProperty
    private Double longitude;

    @JsonProperty
    private Double[] trafficxcam;

    @JsonProperty
    private Double[] trafficycam;

    @JsonProperty
    private Double[] trafficheadingcam;

    @JsonProperty
    private Double[] trafficlengthcam;

    @JsonProperty
    private Double[] trafficspeedxcam;

    @JsonProperty
    private Double[] trafficspeedycam;

    @JsonProperty
    private Long drivetime;

    @JsonProperty
    private Long recordingid;

    @JsonProperty
    private Long driver;

}
