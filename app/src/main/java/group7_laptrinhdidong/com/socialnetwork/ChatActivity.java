package group7_laptrinhdidong.com.socialnetwork;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import group7_laptrinhdidong.com.socialnetwork.adapters.AdapterChat;
import group7_laptrinhdidong.com.socialnetwork.models.ModelChat;
import group7_laptrinhdidong.com.socialnetwork.models.ModelUser;

import group7_laptrinhdidong.com.socialnetwork.notifications.Client;
import group7_laptrinhdidong.com.socialnetwork.notifications.Data;
import group7_laptrinhdidong.com.socialnetwork.notifications.MyResponse;
import group7_laptrinhdidong.com.socialnetwork.notifications.Sender;
import group7_laptrinhdidong.com.socialnetwork.notifications.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    //views
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
    TextView nameTxv, userStatusTxv,txtUser;
    EditText messageEdt;
    ImageButton sendBtn, chooseImage, chooseAudio;
    String imageURL="default", audioURL="default";

    //firebase auth
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    private StorageTask uploadTask;
    private Uri imageUri;
    StorageReference storageReference;

    //for checking if use has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;

    private static final int IMAGE_REQUEST = 1;
    public static MediaPlayer mediaPlayer;
    private int isImage=0;
    public static TextView txtStt;

    APIService apiService;
    boolean notify = false;

    LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
        nameTxv = findViewById(R.id.nameTxv);
        userStatusTxv = findViewById(R.id.usersStatusTxv);
        messageEdt = findViewById(R.id.messageEdt);
        sendBtn = findViewById(R.id.sendBtn);
        chooseImage = findViewById(R.id.chooseImage);
        chooseAudio = findViewById(R.id.chooseAudio);
        txtStt = findViewById(R.id.txtStatus);

        //layout for RecyclerView
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    recyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                recyclerView.smoothScrollToPosition(
                                        recyclerView.getAdapter().getItemCount());
                            }
                            catch (Exception ex){

                            }
                        }
                    }, 100);
                }
            }
        });

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        //api service
        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        //search user to get that user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // check until required info is received

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    //get data
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();

                    //get value of onlinestatus
                    String onlineStatus = ""+ ds.child("onlineStatus").getValue();

                    if(onlineStatus.equals("online")){
                        userStatusTxv.setText(onlineStatus);
                    } else{
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(Long.parseLong(onlineStatus));
                        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
                        userStatusTxv.setText("Last seen at: "+dateTime);
                    }

                    //set data
                    nameTxv.setText(name);
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default).into(profileIv);

                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default).into(profileIv);
                    }

                    readMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //handle button send
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify=true;
                imageURL="default";
                audioURL="default";
                //get text from edit text
                String message = messageEdt.getText().toString().trim();
                //check if text empty or not
                if(TextUtils.isEmpty(message)){
                    Toast.makeText(ChatActivity.this, "Can't send the empty message...", Toast.LENGTH_SHORT).show();

                } else {
                    sendMessage(message, imageURL, audioURL);
                }

                //reset edit text
                messageEdt.setText("");
            }
        });

        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isImage=1;
                openImage();
            }
        });
        chooseAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isImage=0;
                openImage();
            }
        });

        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        seenMessage();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessage() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myUid)&&chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid)&&chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }

                    adapterChat = new AdapterChat(ChatActivity.this,chatList,hisImage);
                    recyclerView.setAdapter(adapterChat);

                    adapterChat.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            linearLayoutManager.smoothScrollToPosition(recyclerView, null, adapterChat.getItemCount());
                        }
                    });

                    adapterChat.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message, String imageURL,String audioURL) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message+"  ");
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("imageURL",imageURL);
        hashMap.put("audioURL",audioURL);
        databaseReference.child("Chats").push().setValue(hashMap);

        if(recyclerView.getAdapter() != null) {
            recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
        }

        final String msg = message;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUser user = dataSnapshot.getValue(ModelUser.class);
                if (notify) {
                    sendNotification(hisUid, user.getName(), msg);
                }
                notify=false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(final String hisUid, final String name, final String message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    Token token =snapshot.getValue(Token.class);
                    Data data = new Data(myUid,R.mipmap.ic_launcher,name+" "+message,"New Message",hisUid);
                    Sender sender = new Sender(data,token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code()==200){
                                        if (response.body().success!=1){
                                            Toast.makeText(ChatActivity.this, "Failt", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Toast.makeText(ChatActivity.this, "Fail", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user!=null){
            myUid = user.getUid();
        }
        else{
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbRef.updateChildren(hashMap);
    }

    public static void TimeOut(){

        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    private void openImage() {
        Intent intent = new Intent();
        if (isImage==1)
            intent.setType("image/*");
        else
            intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = this.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog pd = new ProgressDialog(ChatActivity.this);
        pd.setMessage("Uploading...");
        pd.show();
        if (imageUri!=null){
            final StorageReference fileReference =  storageReference.child(System.currentTimeMillis()+","
                    + getFileExtension(imageUri));
            uploadTask =fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>(){

                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();
                        if (isImage==1)
                            sendMessage(messageEdt.getText().toString(),mUri,"default");
                        else
                            sendMessage(messageEdt.getText().toString(),"default",mUri);
                        pd.dismiss();
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "Thất bại!", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else
        {
            Toast.makeText(this, "Không có image nào được chọn!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //check offline with last seen
        String timestamp = String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        userRefForSeen.removeEventListener(seenListener);
        if (mediaPlayer!=null){
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer=null;
        }
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
        /*mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide search view
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id==R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==IMAGE_REQUEST && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            imageUri=data.getData();
            if (uploadTask!=null && uploadTask.isInProgress()){
                Toast.makeText(this, "Đang tải lên!", Toast.LENGTH_SHORT).show();
            }
            else {
                uploadImage();
            }
        }
    }
}
