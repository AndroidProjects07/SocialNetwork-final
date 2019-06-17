package group7_laptrinhdidong.com.socialnetwork;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.blogspot.atifsoftwares.circularimageview.CircularImageView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import group7_laptrinhdidong.com.socialnetwork.models.ModelUser;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatListFragment extends Fragment {


    //firebase auth
    private RecyclerView myChatList;

    private DatabaseReference ChatListRef, UserRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        //init firebase
        mAuth = FirebaseAuth.getInstance();

        myChatList = view.findViewById(R.id.chat_list_recyclerView);
        myChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        currentUserID = mAuth.getCurrentUser().getUid();

        ChatListRef = FirebaseDatabase.getInstance().getReference().child("Chat List").child(currentUserID);
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<ModelUser>()
                .setQuery(ChatListRef, ModelUser.class)
                .build();


        FirebaseRecyclerAdapter<ModelUser, ChatListViewHolder> adapter
                = new FirebaseRecyclerAdapter<ModelUser, ChatListViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatListViewHolder holder, final int position, @NonNull ModelUser model) {
                String userIDs = getRef(position).getKey();

                UserRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.child("image").getValue().toString().equals("")) {
                            String userImage = dataSnapshot.child("image").getValue().toString();
                            String email = dataSnapshot.child("email").getValue().toString();
                            String userName = dataSnapshot.child("name").getValue().toString();

                            holder.userName.setText(userName);
                            holder.userEmail.setText(email);
                            Picasso.get().load(userImage).placeholder(R.drawable.ic_default).into(holder.profileImage);

                        } else {
                            String email = dataSnapshot.child("email").getValue().toString();
                            String userName = dataSnapshot.child("name").getValue().toString();

                            holder.userName.setText(userName);
                            holder.userEmail.setText(email);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                final String visit_user_id = getRef(position).getKey();

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                        builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which==0) {
                                    //profile clicked
                                    Intent intent = new Intent(getContext(), ThereProfileActivity.class);
                                    intent.putExtra("uid", visit_user_id);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(getContext(), ChatActivity.class);
                                    intent.putExtra("hisUid", visit_user_id);
                                    startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                });


            }

            @NonNull
            @Override
            public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_users, viewGroup, false);
                ChatListViewHolder viewHolder = new ChatListViewHolder(view);
                return viewHolder;
            }
        };

        myChatList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ChatListViewHolder extends RecyclerView.ViewHolder {

        TextView userName, userEmail;
        CircularImageView profileImage;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.nameTxv);
            userEmail = itemView.findViewById(R.id.emailTxv);
            profileImage = itemView.findViewById(R.id.avatarIv);
        }
    }
}