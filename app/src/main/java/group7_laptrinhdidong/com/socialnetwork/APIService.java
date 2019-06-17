package group7_laptrinhdidong.com.socialnetwork;

import group7_laptrinhdidong.com.socialnetwork.notifications.MyResponse;
import group7_laptrinhdidong.com.socialnetwork.notifications.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAHRnebiY:APA91bH9tkEdZ3GewFWJ1gogs4h7KzytJrRCfdBLUP4M6M9AhxRG7EAMCUwF0sFNrq7wcRyPBKU25CbkIr9XfhROfWqIkJPSd0Jy153o20XwegkUl95ECNujzD5g0wCDxLEkae9io4nV"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
