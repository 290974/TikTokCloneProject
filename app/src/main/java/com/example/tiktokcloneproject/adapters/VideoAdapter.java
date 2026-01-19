package com.example.tiktokcloneproject.adapters;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.activity.CommentActivity;
import com.example.tiktokcloneproject.activity.DeleteVideoSettingActivity;
import com.example.tiktokcloneproject.activity.FullScreenAvatarActivity;
import com.example.tiktokcloneproject.activity.MainActivity;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.SettingsAndPrivacyActivity;
import com.example.tiktokcloneproject.activity.VideoActivity;
import com.example.tiktokcloneproject.helper.OnSwipeTouchListener;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.example.tiktokcloneproject.model.Notification;
import com.example.tiktokcloneproject.model.Video;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos;
    private Context context;
    private static FirebaseUser user = null;
    private List<VideoViewHolder> videoViewHolders;
    private int currentPosition;
    private int activePosition = 0; // 🚩 新增：记录当前正处于屏幕中央的位置
    int numberOfClick = 0;
    float volume;
    boolean isPlaying = true;

    public void setActivePosition(int position) {
        this.activePosition = position;
        playVideo(position);
    }

    public VideoAdapter(Context context, List<Video> videos) {
        this.context = context;
        this.videos = videos;
        videoViewHolders = new ArrayList<>();
        currentPosition = 0;

        // 1. 尝试从 Firebase 获取当前真实登录的用户
        // 即使你还没写登录界面，如果之前有登录记录，这行能自动找回身份
        VideoAdapter.user = FirebaseAuth.getInstance().getCurrentUser();

        // 2. 方案 B 的核心：如果用户确实没登录，我们在日志里记录，
        // 但不要在这里拦截，拦截逻辑应该交给 handleTymClick 去做“游客模式”兼容
        if (VideoAdapter.user == null) {
            android.util.Log.d("DEBUG_TAG", "当前为游客模式：双击将仅触发本地动画");
        }
    }

    public static void setUser(FirebaseUser user) {
        VideoAdapter.user = user;
    }

    public void addVideoObject(Video video) {
        this.videos.add(video);
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("ADAPTER_TEST", "onCreateViewHolder 被调用了");
        return new VideoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.video_container, parent, false ));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videos.get(position);

        // 🚩 关键：先确保 holder 被加入列表，再执行 setVideoObjects
        if (!videoViewHolders.contains(holder)) {
            videoViewHolders.add(holder);
        }

        // 这一步会执行你刚改好的 shouldPlay 逻辑
        holder.setVideoObjects(video);

        Log.d("ADAPTER_TEST", "onBindViewHolder 绑定了位置：" + position);
    }

    public void updateCurrentPosition(int pos) {
        currentPosition = pos;

    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void pauseVideo(int position) {
        if (videoViewHolders == null) return;

        // 🚩 同样使用“遍历查找”逻辑
        for (VideoViewHolder holder : videoViewHolders) {
            // 只有当 Holder 的实际绑定位置等于我们要暂停的位置时，才执行操作
            if (holder.getBindingAdapterPosition() == position) {
                holder.pauseVideo();
                Log.d("FIX_LOG", "成功找到并暂停位置: " + position);
                return;
            }
        }

        // 如果没找到 Holder，说明该 View 可能已经被回收了，ExoPlayer 在 onViewRecycled 里已经被 release，
        // 所以这种情况下不报错是正常的。
        Log.w("FIX_LOG", "pauseVideo: 内存中未找到位置 " + position + " 的 Holder，无需手动暂停");
    }

    public void pauseAllVideo() {
        if (videoViewHolders != null) {
            for (VideoViewHolder holder : videoViewHolders) {
                if (holder != null) {
                    holder.pauseVideo();
                }
            }
        }
    }

    public void playVideo(int position) {
        if (videoViewHolders == null) return;

        // 🚩 关键修复：抛弃 get(position)，改用遍历匹配身份
        for (VideoViewHolder holder : videoViewHolders) {
            if (holder != null && holder.getBindingAdapterPosition() == position) {
                holder.playVideo();
                Log.d("ADAPTER_FIX", "【匹配成功】正在播放正确的位置: " + position);
                return;
            }
        }
        Log.w("ADAPTER_FIX", "【匹配失败】内存中尚未找到位置 " + position + " 的 View");
    }

    public void updateWatchCount(int position) {
        if (videoViewHolders != null && position >= 0 && position < videoViewHolders.size()) {
            videoViewHolders.get(position).updateWatchCount();
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int pos = holder.getBindingAdapterPosition();

        // 🚩 当 View 重新贴回屏幕时（比如滑回来）
        // 检查它是不是那个“天选之子” (activePosition)
        if (pos != RecyclerView.NO_POSITION && pos == activePosition) {
            holder.playVideo();
            Log.d("AUDIO_FIX", "ViewAttached: 强制唤醒当前活跃视频 " + pos);
        }
    }

    @Override
    public void onViewDetachedFromWindow(VideoViewHolder holder) {
        holder.pauseVideo();
        Log.d("AUDIO_CONTROL", "Detached: 强制停止位置 " + holder.getAdapterPosition());
//        isPlaying = false;
    }



    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        // 🚩 重要：当 ViewHolder 被回收时，彻底释放播放器，并从管理列表中移除
        if (holder.exoPlayer != null) {
            Log.d("MEMORY_CLEAN", "Recycling player at position: " + holder.getAdapterPosition());
            holder.exoPlayer.release();
            holder.exoPlayer = null;
        }
        videoViewHolders.remove(holder); // 防止列表无限增长
    }



    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        StyledPlayerView videoView;
        ExoPlayer exoPlayer;
        ImageView imvAvatar, imvPause, imvMore, imvAppear, imvVolume, imvShare;
        TextView txvDescription, tvTitle;
        TextView tvComment, tvFavorites;
        ProgressBar pgbWait;
        String authorId;
        String videoId;
        int totalLikes;
        int totalComments;
        DocumentReference docRef;
        FirebaseFirestore db;
        final String LIKE_COLLECTION = "likes";
        String userId;
        boolean isPaused = false;
        boolean isLiked = false;

        Handler handler = new Handler();

        private GestureDetector gestureDetector;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            txvDescription = itemView.findViewById(R.id.txvDescription);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvFavorites = itemView.findViewById(R.id.tvFavorites);
            imvAvatar = itemView.findViewById(R.id.imvAvatar);
            imvPause = itemView.findViewById(R.id.imvPause);
//            pgbWait = itemView.findViewById(R.id.pgbWait);
            imvMore = itemView.findViewById(R.id.imvMore);
            imvAppear = itemView.findViewById(R.id.imv_appear);
            imvVolume = itemView.findViewById(R.id.imvVolume);
            imvShare = itemView.findViewById(R.id.imvShare);
            db = FirebaseFirestore.getInstance();

            videoView.setOnClickListener(this);
            imvAvatar.setOnClickListener(this);
            tvTitle.setOnClickListener(this);
            tvComment.setOnClickListener(this);
            imvMore.setOnClickListener(this);
            tvFavorites.setOnClickListener(this);
            imvVolume.setOnClickListener(this);
            imvShare.setOnClickListener(this);

            // 1. 初始化手势识别（单击/双击）
            gestureDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    togglePlayPause(); // 单击：暂停/播放
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    handleDoubleTap(e); // 双击：红心动画
                    return true;
                }
            });

            // 2. 将手势识别嵌入到 Touch 监听中
            videoView.setOnTouchListener(new OnSwipeTouchListener(itemView.getContext()) {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // 关键：先让 GestureDetector 检查是不是单击或双击
                    gestureDetector.onTouchEvent(event);
                    // 然后再让原来的滑动逻辑（左滑进主页）继续运行
                    return super.onTouch(v, event);
                }

                @Override
                public void onSwipeLeft() {
                    moveToProfile(videoView.getContext(), authorId);
                }
            });

            // 注意：videoView.setOnClickListener(this) 建议删掉，
            // 因为单击事件现在由 onSingleTapConfirmed 接管了。
            // 其他按钮（头像、评论等）的 setOnClickListener 保持不变。
        }
        private void togglePlayPause() {
            if (isPlaying) {
                pauseVideo();
                isPlaying = false;
                imvAppear.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                imvAppear.setVisibility(View.VISIBLE);
            } else {
                playVideo();
                isPlaying = true;
                imvAppear.setVisibility(View.GONE);
            }
        }
        private void handleDoubleTap(MotionEvent e) {
            // 1. 如果还没点赞，触发点赞逻辑（变红、数字加1）
            if (!isLiked) {
                handleTymClick(videoView); // 调用你原来的点赞逻辑
            }

            // 2. 在点击位置弹出红心动画
            showHeartAnimation(e);
        }
        private void showHeartAnimation(MotionEvent e) {
            // 1. 动态创建一个 ImageView
            final ImageView heart = new ImageView(context);
            heart.setImageResource(R.drawable.ic_fill_favorite); // 使用你的红色实心心形

            // 2. 设置红心的尺寸（比如 100x100 像素）
            int size = 300;
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);

            // 3. 计算红心位置（让红心的中心点正好在手指点击处）
            params.leftMargin = (int) e.getX() - (size / 2);
            params.topMargin = (int) e.getY() - (size / 2);
            heart.setLayoutParams(params);

            // 4. 将红心添加到最外层容器中（请确保你 XML 最外层是 RelativeLayout 且有 ID）
            RelativeLayout rootLayout = itemView.findViewById(R.id.video_root_layout); // 替换为你 XML 里的最外层 ID
            rootLayout.addView(heart);

            // 5. 设置属性动画
            // 放大 + 漂浮 + 消失
            heart.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .alpha(0f)
                    .translationY(-300f)
                    .setDuration(800)
                    .withEndAction(() -> rootLayout.removeView(heart)) // 动画结束必须移除，释放内存
                    .start();
        }

        public void playVideo() {
            if (!exoPlayer.isPlaying()) {
                exoPlayer.play();
            }
            if (exoPlayer.getPlaybackState() == Player.STATE_READY
                || exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                    exoPlayer.setPlayWhenReady(true);
                }
            exoPlayer.play();
        }

        public void pauseVideo() {
            if (exoPlayer.getPlaybackState() == Player.STATE_READY) {
                    exoPlayer.setPlayWhenReady(false);
                }
        }

        public void stopVideo() {
            isPaused = true;
            if (exoPlayer.getPlaybackState() == Player.STATE_READY) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.stop();
                exoPlayer.seekTo(0);
            }
        }

        public void appearImage(int src) {
            imvAppear.setImageResource(src);
            imvAppear.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    imvAppear.setVisibility(View.GONE);
                }
            },  1000);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setVideoObjects(final Video videoObject) {
            tvTitle.setText("@" + videoObject.getUsername());
            txvDescription.setText(videoObject.getDescription());
            tvComment.setText(String.valueOf(videoObject.getTotalComments()));
            tvFavorites.setText(String.valueOf(videoObject.getTotalLikes()));
//            videoView.setVideoPath(videoObject.getVideoUri());

            if (exoPlayer != null) exoPlayer.release();
            exoPlayer = new ExoPlayer.Builder(videoView.getContext()).build();
            videoView.setPlayer(exoPlayer);

            MediaItem mediaItem = MediaItem.fromUri(videoObject.getVideoUri());
            exoPlayer.addMediaItem(mediaItem);
            exoPlayer.setRepeatMode(exoPlayer.REPEAT_MODE_ONE);

            exoPlayer.prepare();

            authorId = videoObject.getAuthorId();
            videoId = videoObject.getVideoId();
            totalComments = videoObject.getTotalComments();
            totalLikes = videoObject.getTotalLikes();
            userId = user == null ? "" : user.getUid();

            docRef = db.collection(LIKE_COLLECTION).document(videoId);
//            setVideoViewListener(videoView, imvPause);

            // 🚩 核心修复 1：从数据对象中提取点赞状态和总数
            this.isLiked = videoObject.isUserLiked();
            this.totalLikes = videoObject.getTotalLikes();

            // 🚩 核心修复 2：强行刷新红心状态（解决复用导致的颜色残留）
            setFillLiked(this.isLiked);
            tvFavorites.setText(String.valueOf(this.totalLikes));

            if (userId != null && !userId.isEmpty()) {
                setLikes(videoObject.getVideoId(), userId, videoObject);
            }

            showAvt(imvAvatar, videoObject.getAuthorId());

            if (userId != authorId) {
                imvMore.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == imvAvatar.getId()) {
                moveToProfile(videoView.getContext(), authorId);
                return;
            }
            if(view.getId() == tvTitle.getId()) {
                moveToProfile(videoView.getContext(), authorId);
                return;
            }

            if(view.getId() == tvComment.getId()) {
                if(user == null) {
                    showNiceDialogBox(view.getContext(), null, null);
                    return;
                }
                Intent intent = new Intent(view.getContext(), CommentActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("videoId", videoId);
                bundle.putString("authorId", authorId);
                bundle.putInt("totalComments", totalComments);
                intent.putExtras(bundle);
                view.getContext().startActivity(intent);
                return;
            }
            if (view.getId() == imvMore.getId()) {
                if (user != null && authorId.equals(user.getUid())) {
                    Intent intent = new Intent(view.getContext(), DeleteVideoSettingActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("videoId", videoId);
                    bundle.putString("authorId", authorId);
                    intent.putExtras(bundle);
                    view.getContext().startActivity(intent);
                }
                else {
                    moveToProfile(videoView.getContext(), authorId);
                }
                return;
            }
            if (view.getId() == tvFavorites.getId()) {
                handleTymClick(view);
                return;
            }

            if (view.getId() == imvVolume.getId()) {
                float currentVolume = exoPlayer.getVolume();
                boolean isMuted = (currentVolume == 0);
                if (isMuted) {
                    exoPlayer.setVolume(volume);
                    imvVolume.setImageResource(R.drawable.ic_baseline_volume_up_24);
                    appearImage(R.drawable.ic_baseline_volume_up_24);
                } else {
                    volume = exoPlayer.getVolume();
                    exoPlayer.setVolume(0);
                    imvVolume.setImageResource(R.drawable.ic_baseline_volume_off_24);
                    appearImage(R.drawable.ic_baseline_volume_off_24);
                }
            }
            if (view.getId() == imvShare.getId()) {
                showShareVideoDialog(view);
            }
        }

        public void updateWatchCount() {
            db.collection("profiles").document(authorId)
                    .collection("public_videos").document(videoId).update("watchCount", FieldValue.increment(1));
            final String REGEX_HASHTAG = "#([A-Za-z0-9_-]+)";
            Matcher matcher = Pattern.compile(REGEX_HASHTAG).matcher(txvDescription.getText().toString());
            while(matcher.find()) {
                String hashtag = matcher.group(0);
                db.collection("hashtags").document(hashtag).collection("video_summaries")
                        .document(videoId).update("watchCount", FieldValue.increment(1));
            }

        }

        private void showShareVideoDialog(View view) {
            final Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.share_video_layout);

            Button btnCopyURL = dialog.findViewById(R.id.btnCopyURL);
            Button btnSystemShare = dialog.findViewById(R.id.btnSystemShare);
            TextView txvCancelInSharedPlace = dialog.findViewById(R.id.txvCancelInSharedPlace);


            btnCopyURL.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("toptop-link", "http://video.toptoptoptop.com/" + videoId.toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(view.getContext(), "Video link has been saved to clipboard", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
            });

            if (btnSystemShare != null) {
                btnSystemShare.setOnClickListener(v -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    // 构造分享的文字内容
                    String shareBody = "我在 TopTop 发现了一个有趣的视频，快来看看！\n" +
                            "视频地址：http://video.toptoptoptop.com/" + videoId;
                    sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    sendIntent.setType("text/plain");

                    // 唤起原生选择器 (Intent Chooser)
                    Intent shareIntent = Intent.createChooser(sendIntent, "分享视频到...");
                    v.getContext().startActivity(shareIntent);
                    dialog.dismiss();
                });
            }

            txvCancelInSharedPlace.setOnClickListener(v -> dialog.cancel());

            dialog.show();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }

        private void notifyLike(){
            db.collection("users").document(user.getUid())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String username = document.get("username", String.class);
                                    Notification.pushNotification(username, authorId, StaticVariable.LIKE);
                                    Log.d(ContentValues.TAG, "DocumentSnapshot data: " + document.getData());
                                } else {
                                    Log.d(ContentValues.TAG, "No such document");
                                }
                            } else {
                                Log.d(ContentValues.TAG, "get failed with ", task.getException());
                            }
                        }
                    });


        }


        private void moveToProfile(Context context, String authorId) {
            pauseVideo();
            isPlaying = false;
            imvAppear.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            imvAppear.setVisibility(View.VISIBLE);

            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Video video = videos.get(pos);

            Intent intent=new Intent(context, ProfileActivity.class);
            intent.putExtra("author_id", authorId);
            intent.putExtra("author_name", video.getUsername());

            context.startActivity(intent);
        }

        private void showAvt(ImageView imv, String authorId) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference download = storageRef.child("/user_avatars").child(authorId);

            download.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imv.setImageBitmap(bitmap);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Do nothing
                        }
                    });
        }



        private void setLikes (String videoId, String userId, Video videoObject){
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            isLiked = document.contains(userId);
                            videoObject.setUserLiked(isLiked);
                            setFillLiked(isLiked);
                        } else {
                            isLiked = false;
                            videoObject.setUserLiked(false);
                            setFillLiked(false);
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });

        }

        @SuppressLint("ClickableViewAccessibility")
        private void setVideoViewListener(VideoView videoView, ImageView imvPause) {
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    pgbWait.setVisibility(View.GONE);
                    imvPause.setVisibility(View.GONE);
                    mediaPlayer.start();
                }
            });

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });

            videoView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if(videoView.isPlaying()) {
                        videoView.pause();
                        imvPause.setVisibility(View.VISIBLE);
                        return false;
                    }
                    else {
                        imvPause.setVisibility(View.GONE);
                        videoView.start();
                        return false;
                    }
                }
            });
        }

        private void handleTymClick(View view) {
            // 🚩 找到当前绑定的数据对象
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Video currentVideo = videos.get(pos);

            // 1. 逻辑计算
            if (!isLiked) {
                totalLikes += 1;
            } else {
                if (totalLikes > 0) totalLikes -= 1;
            }
            isLiked = !isLiked;

            // 🚩 核心修复 3：同步更新数据源，这样滑走再滑回来，数据才是对的
            currentVideo.setUserLiked(isLiked);
            currentVideo.setTotalLikes(totalLikes);

            setFillLiked(isLiked); // 更新红心颜色和显示的文字

            // 【第二步：游客模式拦截】
            if (user == null) {
                Log.d(TAG, "游客点赞：仅更新本地UI，不写入数据库");
                return; // 这里直接结束，不再执行后面的 Firebase 代码
            }

            // 【第三步：数据库同步】（只有登录用户才会走到这里）

            // 1. 同步视频的总点赞数
            updateTotalLike(totalLikes);

            // 2. 同步具体的点赞用户记录 (docRef)
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            if (document.contains(userId)) {
                                // 已经点赞过，现在要取消
                                Map<String, Object> updates = new HashMap<>();
                                updates.put(userId, FieldValue.delete());
                                docRef.update(updates);
                            } else {
                                // 没点赞过，现在要加上
                                Map<String, Object> updates = new HashMap<>();
                                updates.put(userId, null);
                                docRef.update(updates); // 修正：建议用 docRef 直接更新
                                notifyLike();
                            }
                        } else {
                            // 整个文档都不存在，新建
                            Map<String, Object> newID = new HashMap<>();
                            newID.put(userId, null);
                            docRef.set(newID);
                            notifyLike();
                        }
                    }
                }
            });
        }

        private void updateTotalLike(int totalLikes) {
            db.collection("videos").document(videoId)
                    .update("totalLikes", totalLikes);
        }

        private void setFillLiked(boolean isLiked) {
            if(isLiked) {
                tvFavorites.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_fill_favorite, 0, 0);
                tvFavorites.setText(String.valueOf(totalLikes));
            }
            else {
                tvFavorites.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_favorite, 0, 0);
                tvFavorites.setText(String.valueOf(totalLikes));
            }
        }

        public void showNiceDialogBox(Context context, @Nullable String title, @Nullable String message) {
            if(title == null) {
                title = context.getString(R.string.request_account_title);
            }
            if(message == null) {
                message = context.getString(R.string.request_account_message);
            }
            try {
                //CAUTION: sometimes TITLE and DESCRIPTION include HTML markers
                AlertDialog.Builder myBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
                myBuilder.setIcon(R.drawable.splash_background)
                        .setTitle(title)
                        .setMessage(message)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                return;
                            }
                        })
                        .setPositiveButton("Sign up/Sign in", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int whichOne) {
                                Intent intent = new Intent(context, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                context.startActivity(intent);
                            }}) //setNegativeButton
                        .show();
            }
            catch (Exception e) { Log.e("Error DialogBox", e.getMessage() ); }
        }

    } // class ViewHolder


}// class adapter