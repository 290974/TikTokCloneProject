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
import androidx.recyclerview.widget.RecyclerView;
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
        if (videoAdapter != null) {
            videoAdapter.pauseAllVideo();
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

        // 🚩 Day 11 新增：设置预加载数量为 1 (预加载前后各一页)
        viewPager2.setOffscreenPageLimit(1);
        // 🚩 Day 11 新增：由于 ViewPager2 默认不开启预加载策略，需要手动开启
        // 这能显著减少由于网速慢导致的“状态 2”黑屏时间
        viewPager2.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER); // 顺便去掉滑到顶的阴影

        if (viewPager2.getChildAt(0) instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) viewPager2.getChildAt(0);
            // 设置缓存大小，防止频繁创建/销毁离屏太近的 ViewHolder
            rv.setItemViewCacheSize(3);
        }

        // --- 这里调用本地加载逻辑 ---
        // 🚩 Day 11：模拟异步网络请求
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            loadVideosFromLocal();
            // 如果有进度条，在这里隐藏：progressBar.setVisibility(View.GONE);
        }, 1500); // 模拟 1.5 秒网络延迟

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                videoAdapter.pauseAllVideo();

                viewPager2.post(() -> {
                    videoAdapter.playVideo(position);
                    videoAdapter.updateCurrentPosition(position);
                });
            }
        });
        viewPager2.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {

            }

            @Override
            public void onViewDetachedFromWindow(View view) {
//                Log.i("position", viewPager2.getVerticalScrollbarPosition() + "");
//               videoAdapter.pauseVideo(videoAdapter.getCurrentPosition());

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

            // 🚩 Day 11 新增：确保刚进页面时，位置 0 的视频能被正确识别
            viewPager2.post(() -> {
                if (videoAdapter.getItemCount() > 0) {
                    viewPager2.setCurrentItem(0, false); // 强制定位到 0
                    videoAdapter.playVideo(0);
                    videoAdapter.updateCurrentPosition(0);
                }
            });
        } catch (Exception e) {
            Log.e("LOCAL_JSON", "读取 JSON 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 在 VideoFragment 类中新增一个方法
    public void onUserStatusChanged(FirebaseUser newUser) {
        this.user = newUser;
        VideoAdapter.setUser(newUser); // 更新静态变量

        // 🚩 关键：只刷新当前正在显示的那个 Item，让它显示出点赞红心
        if (videoAdapter != null) {
            int currentPos = viewPager2.getCurrentItem();
            videoAdapter.notifyItemChanged(currentPos);
            Log.d("DAY_11", "用户登录状态变更，刷新当前视频 UI");
        }
    }

}
