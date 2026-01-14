package com.example.tiktokcloneproject.fragment;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.SearchActivity;
import com.example.tiktokcloneproject.adapters.VideoAdapter;
import com.example.tiktokcloneproject.model.Video;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VideoFragment extends Fragment implements View.OnClickListener {
    private Context context = null;

    private TextView tvVideo; // DE TEST. Sau nay sua thanh clip de xem
    private ViewPager2 viewPager2;
    ArrayList<Video> videos;
    public VideoAdapter videoAdapter;

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    StorageReference storageRef;
    Uri videoUri;

    public static VideoFragment newInstance(String strArg) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = getActivity(); // use this reference to invoke main callbacks
        }
        catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("LIFECYCLE_DEBUG", "onPause: 页面切到后台，尝试暂停视频");

        // 同样的逻辑：只有在有数据时才尝试暂停
        if (videoAdapter != null && videos != null && !videos.isEmpty()) {
            int currentPos = videoAdapter.getCurrentPosition();
            if (currentPos >= 0 && currentPos < videos.size()) {
                videoAdapter.pauseVideo(currentPos);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFECYCLE_DEBUG", "onResume: 页面重新回到前台");

        // --- 核心修复逻辑 ---
        // 1. 检查 videoAdapter 是否已初始化（防止 NullPointerException）
        // 2. 检查 videos 列表是否已经塞入了数据（防止 IndexOutOfBoundsException）
        if (videoAdapter != null && videos != null && !videos.isEmpty()) {
            int currentPos = videoAdapter.getCurrentPosition();

            // 3. 再次确认当前索引是否在列表合法范围内
            if (currentPos >= 0 && currentPos < videos.size()) {
                videoAdapter.playVideo(currentPos);
                Log.d("LIFECYCLE_DEBUG", "成功恢复播放位置：" + currentPos);
            }
        } else {
            // 如果数据还没加载好，代码会走到这里并静默退出，而不再是直接崩溃
            Log.w("LIFECYCLE_DEBUG", "onResume: 数据尚未准备就绪，跳过播放");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 保持你原有的布局加载方式
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_video, null);
        tvVideo = (TextView) layout.findViewById(R.id.tvVideo);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        viewPager2 = layout.findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context, videos); // 变量名是 videoAdapter
        VideoAdapter.setUser(user);

        viewPager2.setAdapter(videoAdapter);

        // --- 这里调用本地加载逻辑 ---
        loadVideosFromLocal();

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                videoAdapter.playVideo(position);
                videoAdapter.updateWatchCount(position);
                videoAdapter.updateCurrentPosition(position);
            }
        });
        viewPager2.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {

            }

            @Override
            public void onViewDetachedFromWindow(View view) {
//                Log.i("position", viewPager2.getVerticalScrollbarPosition() + "");
               videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());

            }
        });

//        loadVideos();
        return layout;
    }

    @Override public void onStart() {
        super.onStart();
        Log.d("LIFECYCLE_DEBUG", "onStart: 视频页面变得可见");
    }

    @Override
    public void onClick(View view) {


    }//on click

    public void pauseVideo() {
        SharedPreferences currentPosPref = context.getSharedPreferences("position", Context.MODE_PRIVATE);
        SharedPreferences.Editor positionEditor = currentPosPref.edit();
        int currentPosition = videoAdapter.getCurrentPosition();
        positionEditor.putInt("position", currentPosition);
        videoAdapter.pauseVideo(currentPosition);
        positionEditor.apply();
    }

    public void continueVideo() {
        SharedPreferences currentPosPref = context.getSharedPreferences("position", Context.MODE_PRIVATE);
        int currentPosition = currentPosPref.getInt("position", -1);
        if (currentPosition != -1) {
            videoAdapter.playVideo(currentPosition);
        }
    }

    private void loadVideos() {
        db.collection("videos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Video video = dc.getDocument().toObject(Video.class);
                                    videos.add(0, video);
                                    videoAdapter.notifyItemInserted(0);
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified city: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                                    break;
                            }
                        }

                    }
                });
    }

    private void loadVideosFromLocal() {
        try {
            InputStream is = getContext().getAssets().open("videos.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                Video video = new Video();
                video.setVideoId(obj.optString("videoId"));
                video.setAuthorId(obj.optString("authorId"));
                video.setUsername(obj.optString("username"));
                video.setDescription(obj.optString("description"));
                video.setVideoUri(obj.optString("videoUri"));
                video.setTotalLikes(obj.optInt("totalLikes", 0));
                video.setTotalComments(obj.optInt("totalComments", 0));

                // 关键点：这里改为 videoAdapter
                videoAdapter.addVideoObject(video);
            }

            // 关键点：这里改为 videoAdapter
            videoAdapter.notifyDataSetChanged();
            Log.d("LOCAL_JSON", "数据加载完成，共计: " + jsonArray.length() + " 条记录");

        } catch (Exception e) {
            Log.e("LOCAL_JSON", "读取 JSON 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
