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
                    "Authorization:key=AAAA5OgQ0Dc:APA91bHyFwNtyoCNGNQRGZCaJjx7Z-_u7LdHiLHtaVIdBCpecz6ZNcIAw1SgYQiEiahUdlsckIBheRw7LpHIjbE8YHgs72x5-0vgQ4HNjoNvus5rPxOKhnhMVnhCNS3OuDlF9EEGWoUj"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
