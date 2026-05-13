package com.lyh.iotcar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/*

暂仅实现八个方向的控制


         A
   H           B

 G       O        C

   F           D
         E


圆心坐标:（ox, oy） 半径：d

A: (ox, oy - d)
B: (ox + d * sin(π/4), oy - d * cos(π/4))
C: (ox + d, oy)
D: (ox + d * cos(π/4), oy + d * sin(π/4))
E: (ox, oy + d)
F: (ox - d * cos(π/4), oy + d * sin(π/4))
G: (ox - d, oy)
H: (ox - d * cos(π/4), oy - d * sin(π/4))

通过向量内积求两个向量之间的角度，找到最接近的点，即为方向
向量模长即为油门大小（做归一化处理）

 */

public class ControlView extends View {

    private Paint mPaint;
    private final PointF mBlueCirclePosition = new PointF();
    private final PointF mTouchOffset = new PointF();
    private boolean isDragging = false;

    private static final int CIRCLE_RATE = 8;


    // cos(π/4) or sin(π/4)
    private final float COS_CONST = 0.7071067811865476f;

    // 时间间隔限制，避免回调过于频繁
    private final long period = 100;

    private long lastTime = 0;


    public interface OnTriggerListener{
        void onTrigger(int direction, float throttle, boolean isStop);
    }

    private OnTriggerListener mOnTriggerListener;

    public void setOnTriggerListener(OnTriggerListener mListener){
        this.mOnTriggerListener = mListener;
    }


    public ControlView(Context context) {
        super(context);
        init();
    }

    public ControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        this.post(new Runnable() {
            @Override
            public void run() {
                mBlueCirclePosition.x = (int)(getWidth() / 2);
                mBlueCirclePosition.y = (int)(getHeight() / 2);
                invalidate();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float ox = getWidth() / 2.0f;
        float oy = getHeight() / 2.0f;
        // 计算大圆的半径，并预留出描边宽度的一半，防止被裁剪
        float d = Math.min(getWidth(), getHeight()) / 2.0f - (float) getWidth() / CIRCLE_RATE;
        float circleRadius = (float) getWidth() / CIRCLE_RATE;

        // 绘制外层红色圆环
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(8f);
        canvas.drawCircle(ox, oy, d, mPaint);

        // 绘制灰色分割线
        drawAnchor(canvas, ox, oy,  d, mPaint);

        // 绘制中间可拖动小圆圈
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.GRAY);

        canvas.drawCircle(mBlueCirclePosition.x, mBlueCirclePosition.y, circleRadius, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float ox = getWidth() / 2.0f;
        float oy = getHeight() / 2.0f;
        float d = Math.min(getWidth(), getHeight()) / 2.0f - (float) getWidth() / CIRCLE_RATE;
        float blueCircleRadius = (float) getWidth() / CIRCLE_RATE;

        if (mBlueCirclePosition.x == 0 && mBlueCirclePosition.y == 0) {
            mBlueCirclePosition.set(ox, oy);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float touchX = event.getX();
                float touchY = event.getY();

                // 判断手指是否按在蓝色小圆内
                double distance = Math.sqrt(Math.pow(touchX - mBlueCirclePosition.x, 2) + Math.pow(touchY - mBlueCirclePosition.y, 2));
                if (distance <= blueCircleRadius) {
                    // 如果按在小圆内，则开始拖动
                    isDragging = true;
                    // 记录手指按下位置与小圆圆心的偏移量
                    mTouchOffset.set(touchX - mBlueCirclePosition.x, touchY - mBlueCirclePosition.y);
                    // 请求重绘
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    // 获取手指当前位置
                    float currentTouchX = event.getX();
                    float currentTouchY = event.getY();

                    // 计算蓝色小圆的新位置
                    float newX = currentTouchX - mTouchOffset.x;
                    float newY = currentTouchY - mTouchOffset.y;

                    handleResult(ox, oy, d, newX, newY);

                    // 限制蓝色小圆的圆心不能超出半径为 d 的范围
                    double distanceFromCenter = Math.sqrt(Math.pow(newX - ox, 2) + Math.pow(newY - oy, 2));
                    if (distanceFromCenter > d) {
                        // 计算边界上的新坐标
                        float angle = (float) Math.atan2(newY - oy, newX - ox);
                        newX = ox + (float) (d * Math.cos(angle));
                        newY = oy + (float) (d * Math.sin(angle));
                    }

                    // 更新蓝色小圆的位置
                    mBlueCirclePosition.set(newX, newY);
                    // 请求重绘
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 【修改】手指抬起或事件取消，将蓝色小圆重置回中心点
                mBlueCirclePosition.set(ox, oy);
                isDragging = false;
                // 请求重绘以显示归位效果
                invalidate();

                if(mOnTriggerListener != null){
                    mOnTriggerListener.onTrigger(0, 0, true);
                }
                break;
        }
        return super.onTouchEvent(event);
    }


    public void drawAnchor(Canvas canvas, float ox, float oy,  float d, Paint mPaint){
        // 绘制灰色分割线
        mPaint.setStrokeWidth(1f);
        mPaint.setColor(Color.GRAY);
        // A
        canvas.drawLine(ox, oy, ox, oy - d, mPaint);
        // B
        canvas.drawLine(ox, oy, (float) (ox + d * COS_CONST), (float) (oy - d * COS_CONST), mPaint);
        // C
        canvas.drawLine(ox, oy, ox + d, oy, mPaint);
        // D
        canvas.drawLine(ox, oy, (float) (ox + d * COS_CONST), (float) (oy + d * COS_CONST), mPaint);
        // E
        canvas.drawLine(ox, oy, ox, oy + d, mPaint);
        // F
        canvas.drawLine(ox, oy, (float) (ox - d * COS_CONST), (float) (oy + d * COS_CONST), mPaint);
        // G
        canvas.drawLine(ox, oy, ox - d, oy, mPaint);
        // H
        canvas.drawLine(ox, oy, (float) (ox - d * COS_CONST), (float) (oy - d * COS_CONST), mPaint);
    }

    public void handleResult(float ox, float oy, float d, float curX, float curY) {

        PointF[] points = new PointF[]{
                new PointF(ox, oy - d),
                new PointF( ox + d * COS_CONST, oy - d * COS_CONST),
                new PointF(ox + d, oy),
                new PointF(ox + d * COS_CONST, oy + d * COS_CONST),
                new PointF(ox, oy + d),
                new PointF(ox - d * COS_CONST, oy + d * COS_CONST),
                new PointF(ox - d, oy),
                new PointF(ox - d * COS_CONST, oy - d * COS_CONST)
        };

        float min = Float.MAX_VALUE;
        int minIndex = -1;
        double modulus = 0;
        for(int i=0; i<points.length; i++){
            float vectorX = points[i].x - ox;
            float vectorY = points[i].y - oy;

            float cX = curX - ox;
            float cY = curY - oy;
            modulus = Math.sqrt(Math.pow(cX, 2) + Math.pow(cY, 2));
            double ret = Math.acos((vectorX * cX + vectorY * cY) / (Math.sqrt(Math.pow(vectorX, 2) + Math.pow(vectorY, 2)) * modulus));
            if(ret < min){
                min = (float) ret;
                minIndex = i;
            }
        }

        if(mOnTriggerListener != null){
            float throttle = (float) (modulus / d);
            if(throttle > 1.0f){
                throttle = 1.0f;
            }
            if(System.currentTimeMillis() - lastTime > period){
                mOnTriggerListener.onTrigger(minIndex, throttle, false);
                lastTime = System.currentTimeMillis();
            }
        }
    }
}