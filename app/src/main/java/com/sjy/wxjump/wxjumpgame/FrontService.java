package com.sjy.wxjump.wxjumpgame;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Created by sjy on 2018/1/15.
 */

public class FrontService extends Service {
    private Handler myHander;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getExtras()!=null){
            if(myHander==null) {
                myHander = new Handler();
            }
            myHander.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("ingame", "截图");
                    try {
                        CommandExecution.execCommand("screencap -p /sdcard/跳一跳助手/wxjump.png",true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("error", e.getMessage());
                    }
                    dealImg();
                }
            }, 5000);

        }else {
            RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.view_notification);
            Intent intentStart = new Intent();
            intentStart.setClass(this, FrontService.class);
            intentStart.putExtra("type", "start");
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 200, intentStart, 0);
            remoteViews.setOnClickPendingIntent(R.id.start, pendingIntent);
            Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_round))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setCustomContentView(remoteViews)
                        .setWhen(System.currentTimeMillis());
            }
            Notification notification = builder.build();
            notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
            startForeground(110, notification);//开始前台服务
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("service","跳一跳service停止了");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.e("service","内存过低，可能被回收");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e("service","taskRemoved");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("service","unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onTrimMemory(int level) {
        Log.e("service","trimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("service","configChanged");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 计算棋子的位置点和最新跳台的位置点
     */
    private void dealImg(){
        Bitmap src =  BitmapFactory.decodeFile("/sdcard/跳一跳助手/wxjump.png");
        int  R, G, B;
        int pixelColor;
        int pixelColorTop=0;//台面上顶点颜色
        int pixelBottomColor=0;//台面下顶点颜色
        int height = src.getHeight();
        int width = src.getWidth();
        Point chessPoint=new Point();
        Point tableTopPoint=new Point();
        Point tableMiddlePoint=new Point();
        Point tableBottomPoint=new Point();
        //寻找跳台上顶点
        searchTop:
        for (int y = height/4; y < 2*height/3; y++) {
            int pixelColorBorder=src.getPixel(50, y);//边界对照颜色
            int R_BORDER= Color.red(pixelColorBorder);
            int G_BORDER= Color.green(pixelColorBorder);
            int B_BORDER= Color.blue(pixelColorBorder);
            for (int x = 50; x < width-50; x++) {
                pixelColor = src.getPixel(x, y);
                R = Color.red(pixelColor);
                G = Color.green(pixelColor);
                B = Color.blue(pixelColor);
                //根据颜色值差异判断上顶点
                if(Math.abs(R_BORDER-R)>10||Math.abs(G_BORDER-G)>10||Math.abs(B_BORDER-B)>10){
                    pixelColorTop=pixelColor;
                    tableTopPoint.x=x;
                    tableTopPoint.y=y;
                    Log.e("tableTopPointColor:","("+R+"|"+G+"|"+B+")");
                    break searchTop;
                }

            }
        }
        //寻找棋子坐标点
        searchChess:
        for(int y=tableTopPoint.y;y<2*height/3; y++){
            for (int x = 50; x < width-50; x++) {
                pixelColor = src.getPixel(x, y);
                R = Color.red(pixelColor);
                G = Color.green(pixelColor);
                B = Color.blue(pixelColor);
                //根据颜色值判断棋子上定顶点
                if(50 < R&&R< 60&&53 < G &&G< 63&&95 < B&&B< 110){
                    chessPoint.x=x;
                    chessPoint.y=y+130;
                    Log.e("chess:",chessPoint.x+"|"+chessPoint.y);
                    Log.e("chess:",R+"|"+G+"|"+B);
                    break searchChess;
                }
            }
        }
        //寻找跳台下顶点,从最大方块往上计算，寻找与上顶点相同的点
        for(int y=tableTopPoint.y+274;y>tableTopPoint.y; y--){
            pixelBottomColor = src.getPixel(tableTopPoint.x, y);
            if(pixelBottomColor==pixelColorTop){
                tableBottomPoint.x=tableTopPoint.x;
                tableBottomPoint.y=y;
                tableMiddlePoint.x=tableBottomPoint.x;
                tableMiddlePoint.y=(tableBottomPoint.y+tableTopPoint.y)/2;
                Log.e("bottom:",tableTopPoint.x+"|"+y);
                Log.e("middle:",tableMiddlePoint.x+"|"+tableMiddlePoint.y);
                break;
            }

        }
        if(src != null && !src.isRecycled()){
            // 回收
            src.recycle();
            src = null;
        }
        Double dis= distanceBetweenPoints(chessPoint,tableMiddlePoint);
        jumpToNextTable(dis);
    }

    private Double distanceBetweenPoints(Point point,Point point2){
        return Math.sqrt((point2.y-point.y)*(point2.y-point.y)+(point2.x-point.x)*(point2.x-point.x));
    }

    //跳
    private void jumpToNextTable(Double distance){
        float SpaceTimeConfig=0.92f;
        Log.e("distance",(int)(SpaceTimeConfig*distance)+"");
        try {
            CommandExecution.execCommand("input swipe 100 100 100 100 "+(int)(SpaceTimeConfig*distance),true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error",e.getMessage()+"");
        }
        myHander.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("ingame", "截图");
                try {
                    CommandExecution.execCommand("screencap -p /sdcard/跳一跳助手/wxjump.png",true);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("error", e.getMessage());
                }
                dealImg();
            }
        }, 5000);
    }

}
