package com.example.px.videoapp;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class FixVideoView extends VideoView {

    private int mVideoWidth;
    private int mVideoHeight;

    public FixVideoView(Context context) {
        super(context);
    }

    public FixVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public  void  setSize(int width,int height){
        this.mVideoWidth = width;
        this.mVideoHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	/* The following code is to make videoView view length-width
    	based on the parameters you set to decide. */
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

}
