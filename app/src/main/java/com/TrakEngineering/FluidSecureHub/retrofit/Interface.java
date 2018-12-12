package com.TrakEngineering.FluidSecureHub.retrofit;

import com.squareup.okhttp.RequestBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Interface {

    //@Headers("Content-Type: text/html; charset=utf-8")
    @Headers("Content-Type: application/json")
    @POST("HandlerTrak.ashx")
    Call<ServerResponse> postttt(@Header("Authorization") String authString,
                              @Body String jsonData
                              //@Query("body") String jsonData


    );

    @Headers("Content-Type: application/json")
    @POST("HandlerTrak.ashx")
    Call<ServerResponse> post(@Header("Authorization") String authString,
                                @Body String jsonData
                                //@Query("body") String jsonData




    );



/*    //This method is used for "POST"
    @FormUrlEncoded
    @POST("HandlerTrak.ashx")
    Call<ServerResponse> post(
            //@Field("method") String method,
//            @Field("username") String username,
//            @Field("password") String passw
            @Header("Authorization") String authString,
            @Body String jsonData

    );*/

    //This method is used for "GET"
    @GET("HandlerTrak.ashx")
    Call<ServerResponse> get(
//            @Query("method") String method,
//            @Query("Email") String Email,
//            @Query("FSNPMacAddress") String FSNPMacAddress,
//            @Query("FSTagMacAddress") String FSTagMacAddress


    );

}
