package group7_laptrinhdidong.com.socialnetwork.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import group7_laptrinhdidong.com.socialnetwork.ChatActivity;
import group7_laptrinhdidong.com.socialnetwork.R;
import group7_laptrinhdidong.com.socialnetwork.models.ModelChat;

public class AdapterChat extends  RecyclerView.Adapter<AdapterChat.MyHolder>{

    private static final int MSG_TYPE_LEFT =0;
    private static final int MSG_TYPE_RIGHT =1;
    public static final int MSG_TYPE_LEFT_IMAGE=2;
    public static final int MSG_TYPE_RIGHT_IMAGE=3;
    public static final int MSG_TYPE_LEFT_AUDIO=4;
    public static final int MSG_TYPE_RIGHT_AUDIO=5;
    public static int pos=-1,curr=-1,stop=0,playpause=-1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;
    boolean isPlay=false;
    ProgressDialog progressDialog;
    RecyclerView.ViewHolder vieww;

    //firebase
    FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layouts: row_chat_left.xml for receiver, right for sender
        if(i==MSG_TYPE_LEFT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, viewGroup, false);
            return new MyHolder(view);
        } else if (i==MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, viewGroup, false);
            return new MyHolder(view);
        } else if (i==MSG_TYPE_RIGHT_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_image_right, viewGroup, false);
            return new MyHolder(view);
        } else if (i==MSG_TYPE_LEFT_IMAGE){
            View view = LayoutInflater.from(context).inflate(R.layout.chat_image_left, viewGroup, false);
            return new MyHolder(view);
        } else if (i==MSG_TYPE_RIGHT_AUDIO){
            View view = LayoutInflater.from(context).inflate(R.layout.chat_audio_right, viewGroup, false);
            return new MyHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_audio_left, viewGroup, false);
            return new MyHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder myHolder, final int i) {
        isPlay=false;
        String timeStamp = chatList.get(i).getTimestamp();

        if (!chatList.get(i).getImageURL().equals("default"))
            Picasso.get().load(chatList.get(i).getImageURL()).into(myHolder.imgChat);
        else if (chatList.get(i).getAudioURL().equals("default")){
            myHolder.messageTxv.setText(chatList.get(i).getMessage());
        }
        //cover time stamp to dd/mm/yyyy hh:mm am/pm
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timeStamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
        myHolder.timeTxv.setText(dateTime);
        try{
            Picasso.get().load(imageUrl).into(myHolder.profileIv);
        } catch (Exception e){

        }
        if (progressDialog==null)
            progressDialog = new ProgressDialog(context);


        if(i==chatList.size()-1){
            if (chatList.get(i).isSeen()){
                myHolder.isSeenTxv.setText("Seen");
            } else {
                myHolder.isSeenTxv.setText("Delivered");
            }
        } else {
            myHolder.isSeenTxv.setVisibility(View.GONE);
        }

        if (!chatList.get(i).getAudioURL().equals("default")) {
            if (stop==1){
                myHolder.imgPause.setVisibility(View.INVISIBLE);
                myHolder.imgPlay.setVisibility(View.VISIBLE);
                myHolder.txtStatus.setText("Start Play");
            }
            myHolder.imgPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    curr=i;
                    if (ChatActivity.mediaPlayer==null){
                        playpause=i;
                        ChatActivity.mediaPlayer = new MediaPlayer();
                        ChatActivity.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    }
                    else
                    {
                        if (ChatActivity.mediaPlayer.isPlaying()||playpause!=i){

                            ChatActivity.mediaPlayer.stop();
                            ChatActivity.mediaPlayer.release();
                            ChatActivity.mediaPlayer=new MediaPlayer();
                            ChatActivity.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            isPlay = false;
                            if (pos!=-1&&pos!=i){
                                stop=1;
                                notifyItemChanged(pos);
                            }
                        }
                    }
                    pos=i;
                    myHolder.imgPlay.setVisibility(View.INVISIBLE);
                    myHolder.imgPause.setVisibility(View.VISIBLE);
                    myHolder.txtStatus.setText("Pause");
                    if (!isPlay){
                        new Player().execute(chatList.get(i).getAudioURL());
                        ChatActivity.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                isPlay=false;
                                myHolder.imgPlay.setVisibility(View.VISIBLE);
                                myHolder.imgPause.setVisibility(View.INVISIBLE);
                                myHolder.txtStatus.setText("Start Play");
                                ChatActivity.TimeOut();
                            }
                        });
                    }
                    else {
                        if (!ChatActivity.mediaPlayer.isPlaying()){
                            ChatActivity.mediaPlayer.start();
                        }
                    }
                }
            });
            myHolder.imgPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playpause=i;
                    myHolder.txtStatus.setText("Play");
                    myHolder.imgPlay.setVisibility(View.VISIBLE);
                    myHolder.imgPause.setVisibility(View.INVISIBLE);
                    if (ChatActivity.mediaPlayer.isPlaying()) {
                        ChatActivity.mediaPlayer.pause();
                    }
                }
            });
        }
    }

//    private void deleteMessage(int position){
//
//        final String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        String msgTimeStamp = chatList.get(position).getTimestamp();
//        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
//        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
//        query.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for(DataSnapshot ds: dataSnapshot.getChildren()){
//
//                    if(ds.child("sender").getValue().equals(myUID)){
//
//                        ds.getRef().removeValue();
//
//                        Toast.makeText(context, "message deleted...", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(context, "You can delete only your message...", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//
//    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //get currently signed in user
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid()))
        {
            if (!chatList.get(position).getImageURL().equals("default"))
                return MSG_TYPE_RIGHT_IMAGE;
            else if (!chatList.get(position).getAudioURL().equals("default"))
                return MSG_TYPE_RIGHT_AUDIO;
            else
                return MSG_TYPE_RIGHT;
        }
        else {
            if (!chatList.get(position).getImageURL().equals("default"))
                return MSG_TYPE_LEFT_IMAGE;
            else if (!chatList.get(position).getAudioURL().equals("default"))
                return MSG_TYPE_LEFT_AUDIO;
            else return MSG_TYPE_LEFT;
        }
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views
        ImageView profileIv;
        TextView messageTxv, timeTxv, isSeenTxv, txtStatus;
        LinearLayout messageLayout;
        ImageView imgChat, imgPlay, imgPause;

        public MyHolder(@NonNull View itemView){
            super(itemView);

            //init views
            profileIv = itemView.findViewById(R.id.profileIv);
            messageTxv = itemView.findViewById(R.id.messageTxv);
            timeTxv = itemView.findViewById(R.id.timeTxv);
            isSeenTxv = itemView.findViewById(R.id.isSeenTxv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            imgChat = itemView.findViewById(R.id.imgChat);
            imgPlay = itemView.findViewById(R.id.imgPlay);
            imgPause = itemView.findViewById(R.id.imgPause);
            txtStatus= itemView.findViewById(R.id.txtStatus);
        }
    }

    //player class
    class Player extends AsyncTask<String,Void,Boolean>{
        @Override
        protected Boolean doInBackground(String... strings) {

            Boolean prepared =false;
            try {

                ChatActivity.mediaPlayer.setDataSource(strings[0]);
                ChatActivity.mediaPlayer.prepare();
                prepared=true;
            }
            catch (IOException ex){
                Log.e("My audio app ", ex.getMessage());
                prepared=false;
            }
            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (progressDialog.isShowing()){
                progressDialog.cancel();
            }
            ChatActivity.mediaPlayer.start();
            isPlay=true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }
    }
}
