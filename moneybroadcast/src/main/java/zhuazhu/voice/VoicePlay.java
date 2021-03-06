package zhuazhu.voice;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import zhuazhu.voice.constant.VoiceConstants;
import zhuazhu.voice.utils.FileUtils;

/**
 * 创建时间: 2017/12/26 16:47<br/>
 * 创建人: 李涛<br/>
 * 修改人: 李涛<br/>
 * 修改时间: 2017/12/26 16:47<br/>
 * 描述: 语音播报
 */

public class VoicePlay {

    private ExecutorService mExecutorService;
    private Context mContext;

    private VoicePlay(Context context) {
        this.mContext = context;
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    private volatile static VoicePlay mVoicePlay = null;

    /**
     * 单例
     *
     * @return
     */
    public static VoicePlay with(Context context) {
        if (mVoicePlay == null) {
            synchronized (VoicePlay.class) {
                if (mVoicePlay == null) {
                    mVoicePlay = new VoicePlay(context);
                }
            }
        }
        return mVoicePlay;
    }

    /**
     * 退款成功
     */
    public void playRefundSuccess(){
        executeStart("refund_success");
    }
    /**
     * 收款失败
     */
    public void playDefeated(){
        executeStart("defeated");
    }
    /**
     * 关闭语音播报
     */
    public void playClose(){
        executeStart("close");
    }
    /**
     * 播报收款成功
     */
    public void playSuccess(){
        executeStart("suc");
    }
    private void executeStart(final String voice){
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                List<String> voicePlay = new ArrayList<>();
                voicePlay.add(voice);
                start(voicePlay);
            }
        });
    }
    /**
     * 播报收到+金额
     *
     * @param money
     */
    public void play(String money) {
        VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                .start(VoiceConstants.GOLD,VoiceConstants.SUCCESS)
                .money(money)
                .unit(VoiceConstants.YUAN)
                .builder();
        executeStart(voiceBuilder);
    }

    /**
     * 播报金额
     * @param money
     */
    public void playMoney(String money){
        VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                .money(money)
                .unit(VoiceConstants.YUAN)
                .builder();
        executeStart(voiceBuilder);
    }

    /**
     * 接收自定义
     *
     * @param voiceBuilder
     */
    public void play(VoiceBuilder voiceBuilder) {
        executeStart(voiceBuilder);
    }

    /**
     * 开启线程
     *
     * @param builder
     */
    private void executeStart(VoiceBuilder builder) {
        final List<String> voicePlay = VoiceTextTemplate.genVoiceList(builder);
        if (voicePlay == null || voicePlay.isEmpty()) {
            return;
        }
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                start(voicePlay);
            }
        });
    }

    /**
     * 开始播报
     *
     * @param voicePlay
     */
    private void start(final List<String> voicePlay) {
        synchronized (VoicePlay.this) {

            MediaPlayer mMediaPlayer = new MediaPlayer();
            final CountDownLatch mCountDownLatch = new CountDownLatch(1);
            AssetFileDescriptor assetFileDescription = null;

            try {
                final int[] counter = {0};
                assetFileDescription = FileUtils.getAssetFileDescription(mContext,
                        String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                mMediaPlayer.setDataSource(
                        assetFileDescription.getFileDescriptor(),
                        assetFileDescription.getStartOffset(),
                        assetFileDescription.getLength());
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.reset();
                        counter[0]++;

                        if (counter[0] < voicePlay.size()) {
                            try {
                                AssetFileDescriptor fileDescription2 = FileUtils.getAssetFileDescription(mContext,
                                        String.format(VoiceConstants.FILE_PATH, voicePlay.get(counter[0])));
                                mediaPlayer.setDataSource(
                                        fileDescription2.getFileDescriptor(),
                                        fileDescription2.getStartOffset(),
                                        fileDescription2.getLength());
                                mediaPlayer.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                            }
                        } else {
                            mediaPlayer.release();
                            mCountDownLatch.countDown();
                        }
                    }
                });



            } catch (Exception e) {
                e.printStackTrace();
                mCountDownLatch.countDown();
            } finally {
                if (assetFileDescription != null) {
                    try {
                        assetFileDescription.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                mCountDownLatch.await();
                notifyAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
