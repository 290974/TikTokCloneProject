package com.example.tiktokcloneproject.activity;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.CommentAdapter;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

public class CommentActivity extends Activity implements View.OnClickListener{
    private ImageView imvBack, imvMyAvatarInComment;
    private LinearLayout llComment;
    private EditText edtComment;
    private ImageButton imbSendComment;
    private String videoId, userId, avatarName;
    private Uri avatarUri;
    private Bitmap bitmap;
    private ListView lvComment;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageRef, imagesRef;
    DocumentReference docRef;
    String username;
    String authorVideoId;
    int totalComments;
    CommentAdapter adapter;

    Handler handler = new Handler();

    ArrayList<Comment> comments;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // 1. 基本绑定
        llComment = findViewById(R.id.llComment);
        imvBack = findViewById(R.id.imvBackToHomeScreen);
        imvMyAvatarInComment = findViewById(R.id.imvMyAvatarInComment);
        edtComment = findViewById(R.id.edtComment);
        imbSendComment = findViewById(R.id.imbSendComment);
        lvComment = findViewById(R.id.listViewComment);

        // 2. 数据获取与判空
        Bundle bundle = getIntent().getExtras();
        videoId = (bundle != null) ? bundle.getString("videoId") : "default_video";

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        comments = new ArrayList<>();
        adapter = new CommentAdapter(this, R.layout.layout_row_comment, comments);

        // 3. 模拟数据（保留这个，方便你检查 UI）
        for (int i = 0; i < 15; i++) {
            Comment mc = new Comment();
            mc.setContent("测试评论 " + (i + 1));
            comments.add(mc);
        }
        lvComment.setAdapter(adapter);

        imvBack.setOnClickListener(this);
        imbSendComment.setOnClickListener(this);

        // 4. 【核心保护】只执行一次 Firebase 用户逻辑
        if (user != null) {
            userId = user.getUid();
            String uid = user.getUid();
            // 获取头像
            storageRef.child("/user_avatars").child(uid).getBytes(1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imvMyAvatarInComment.setImageBitmap(bitmap);
                    });
        }

        // 5. 监听评论
        db.collection("comments").whereEqualTo("videoId", videoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            comments.add(0, dc.getDocument().toObject(Comment.class));
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
        // onCreate 结束，后面原本重复的那几十行代码已经全部清理掉了
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onClick(View v){
        if (v.getId() == imvBack.getId()){
            onBackPressed();
        }
        if (v.getId() == imbSendComment.getId()){
            String cmt = edtComment.getText().toString().trim();
            if (TextUtils.isEmpty(cmt))
            {
//                Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
                return;
            }
            String timeStamp = String.valueOf(System.currentTimeMillis());
            Comment comment = new Comment(timeStamp, videoId, userId, cmt);
            postComment(comment);
            edtComment.setText("");
        }
    }

    private void postComment(Comment comment ) {
        Map<String, Object> values = comment.toMap();

        db.collection("comments").document(comment.getCommentId()).set(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Notification.pushNotification(username, authorVideoId, StaticVariable.COMMENT);
                    handler.post(CommentActivity.this::updateTotal);
//                    Toast.makeText(CommentActivity.this, "Comment successfully!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(CommentActivity.this, "Comment fail!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateTotal() {
        db.collection("videos").document(videoId)
                .update("totalComments", totalComments + 1)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_left_to_right, R.anim.slide_out_bottom);
    }
}
