package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.example.tiktokcloneproject.model.Notification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private ArrayList<Comment> comments;
    private Context context;

    public CommentAdapter(@NonNull Context context, int resource, ArrayList<Comment> comments) {
        super(context, resource, comments);
        this.comments = comments;
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.layout_row_comment, null);

        }



        ImageView imvAvatarInComment = (ImageView) row.findViewById(R.id.imvAvatarInComment);
        TextView txvUsernameInComment = (TextView) row.findViewById(R.id.txvUsernameInComment);
        TextView txvComment = (TextView) row.findViewById(R.id.txvComment);
        TextView txvTotalLikeComment = (TextView) row.findViewById(R.id.txvTotalLikeComment);



        String authorId = comments.get(position).getAuthorId();

        if (authorId != null && !authorId.isEmpty()) {
            // 这行是原来的 72 行，现在它在 if 里面，非常安全
            DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(authorId);

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            txvUsernameInComment.setText("@" + document.get("username", String.class));
                        } else {
                            txvUsernameInComment.setText("@unknown");
                        }
                    }
                }
            });

            // 加载头像
            loadAvatar(authorId, imvAvatarInComment);

        } else {
            // 如果作者 ID 根本不存在（导致崩溃的元凶），走这里
            txvUsernameInComment.setText("@anonymous");
        }

        txvComment.setText(comments.get(position).getContent());

        if (!row.hasOnClickListeners()) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 1. 获取当前点击的评论数据
                    Comment currentComment = comments.get(position);

                    // 2. 创建快递员 (Intent)
                    Intent intent = new Intent(view.getContext(), ProfileActivity.class);

                    // 3. 往包裹里塞入“假数据”
                    // 即使没网，我们也把 ID 和一个测试名字传过去
                    intent.putExtra("id", currentComment.getAuthorId());
                    intent.putExtra("mock_name", "测试用户_" + currentComment.getAuthorId());

                    // 4. 出发！
                    view.getContext().startActivity(intent);
                }
            });
        }

        if(!row.hasOnLongClickListeners()) {
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) context
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    final android.content.ClipData clipData = android.content.ClipData
                            .newPlainText("copy comment", txvComment.getText().toString());
                    clipboardManager.setPrimaryClip(clipData);

                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }


        txvTotalLikeComment.setText(comments.get(position).getTotalLikes() + "");

        return (row);
    }

    private void loadAvatar(String authorId, ImageView imv) {
        StorageReference download = FirebaseStorage.getInstance().getReference().child("/user_avatars").child(authorId);
//                        StorageReference download = storageRef.child(userId.toString());

        download.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);
                        imv.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Do nothing
                    }
                });
//                    }
//                    else { }
//                }
//                else { }
//            });
    }
}
