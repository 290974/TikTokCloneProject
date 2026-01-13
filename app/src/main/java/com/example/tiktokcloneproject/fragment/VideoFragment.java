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
        if (videoAdapter != null) {
            // 使用你代码里已有的逻辑，暂停当前位置的视频
            videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFECYCLE_DEBUG", "onResume: 页面重新回到前台");
        if (videoAdapter != null) {
            // 自动恢复当前位置的播放
            videoAdapter.playVideo(videoAdapter.getCurrentPosition());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
// inflate res/layout_blue.xml to make GUI holding a TextView and a ListView
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_video, null);
        tvVideo = (TextView) layout.findViewById(R.id.tvVideo);





        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();



/////////////////////////////////////////////////////////////////////////
        viewPager2 = layout.findViewById(R.id.viewPager);
        videos = new ArrayList<>();
        videoAdapter = new VideoAdapter(context, videos);
        VideoAdapter.setUser(user);

        // --- 插入点：在 setAdapter 之前 ---
        Video test1 = new Video();
        test1.setTitle("测试视频 1：如果你能看到这个文字");
        test1.setVideoUri("https://www.w3schools.com/html/mov_bbb.mp4");// 这是一个公用的 mp4 测试链接
        test1.setAuthorId("test_user_01");
        test1.setVideoId("id_001"); // <--- 补上这一行，防止第 310 行崩掉

        Video test2 = new Video();
        test2.setTitle("测试视频 2：ViewPager2 翻页测试");
        test2.setVideoUri("https://www.w3schools.com/html/movie.mp4");
        test2.setAuthorId("test_user_02");
        test2.setVideoId("id_002"); // <--- 同理，补上这一行

// 把假数据加入列表
        videos.add(test1);
        videos.add(test2);

        viewPager2.setAdapter(videoAdapter);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);

            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());
                videoAdapter.playVideo(position);
                videoAdapter.updateWatchCount(position);
                Log.e("Selected_Page", String.valueOf(videoAdapter.getCurrentPosition()));
                videoAdapter.updateCurrentPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
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

}
