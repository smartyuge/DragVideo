/*
 * Copyright (C) 2016 hejunlin <hejunlin2013@gmail.com>
 * 
 * Github:https://github.com/hejunlin2013/DragVideo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hejunlin.dragvideo;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity implements DragVideoView.Callback, MediaPlayer.OnPreparedListener, TextureView.SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextureView mVideoView;
    private MediaPlayer mMediaPlayer;
    private DragVideoView mDragVideoView;
    private ListView mDetailInfoListView;
    private ListView mProgramListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgramListView = (ListView) findViewById(R.id.lv_program);
        mProgramListView.setAdapter(ArrayAdapter.createFromResource(this, R.array.program_list, android.R.layout.simple_list_item_1));
        mProgramListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mProgramListView.setVisibility(View.GONE);
                playVideo();
            }
        });
        mVideoView = (TextureView) findViewById(R.id.video_view);
        mVideoView.setSurfaceTextureListener(this);
        mMediaPlayer = MediaPlayer.create(this,R.raw.test_4);
        mMediaPlayer.setOnPreparedListener(this);
        mDragVideoView = (DragVideoView) findViewById(R.id.drag_view);
        mDragVideoView.setCallback(this);
        mDetailInfoListView = (ListView) findViewById(R.id.lv_info);
        mDetailInfoListView.setAdapter(ArrayAdapter.createFromResource(this, R.array.info_list, android.R.layout.simple_list_item_1));
    }

    private void playVideo() {
        mDragVideoView.show();
        if (mMediaPlayer.isPlaying())
            return;
        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaPlayer.start();
    }

    @Override
    public void onDisappear(int direct) {
        mMediaPlayer.pause();
        mProgramListView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.setLooping(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mMediaPlayer.setSurface(new Surface(surface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, ">> onSurfaceTextureSizeChanged width=" + width + ", height=" + height);
        if (width == 540 && height == 303) {
            mProgramListView.setAlpha(1.0f);
        } else {
            float f = (float) ((1.0 - ((float)width/1080))* 1.0f);
            Log.d(TAG, ">> onSurfaceTextureSizeChanged f=" + f );
            mProgramListView.setAlpha(f);
        }
        mProgramListView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        finish();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}