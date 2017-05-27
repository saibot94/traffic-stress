package com.hacktm17.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacktm17.dto.CarJsonDTO;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.List;

/**
 * Created by darkg on 27-May-17.
 */
public class HttpDataPost {

    private static final String URL = "localhost:9000/carData";

    public void insertMany(List<CarJsonDTO> dtos) {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead
        try {
            System.out.println("Len: " + dtos.size());
            ObjectMapper mapper = new ObjectMapper();
            HttpPost request = new HttpPost("http://" + URL);
            StringEntity params =new StringEntity(mapper.writeValueAsString(dtos));
            request.addHeader("content-type", "application/json");
            request.addHeader("Accept","application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Error sending data");
            }
            // handle response here...
        }catch (Exception ex) {
            // handle exception here
            System.out.println("Error sending data");
        } finally {
            httpClient.getConnectionManager().shutdown(); //Deprecated
        }
    }
}
