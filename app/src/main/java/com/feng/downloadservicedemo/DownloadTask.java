package com.feng.downloadservicedemo;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Feng Zhaohao
 * Created on 2018/9/24
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    //AsyncTask的三个参数:
    //第一个参数表示需要传一个字符串参数给后台任务
    //第二个参数表示使用整形参数作为进度显示单位
    //第三个参数表示使用整形参数来反馈执行结果

    //表示下载状态
    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int PAUSED = 2;
    public static final int CANCELED = 3;

    private DownloadListener mDownloadListener;     //待会的下载状态要通过该接口回调

    private boolean isCanceled = false;
    private boolean isPaused = false;

    private int mLastProgress = 0;

    public DownloadTask(DownloadListener mDownloadListener) {
        this.mDownloadListener = mDownloadListener;
    }

    /**
     * 执行耗时操作，这里在后台执行下载操作
     * @param strings
     * @return
     */
    @Override
    protected Integer doInBackground(String... strings) {
        InputStream inputStream = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try {
            long downloadedLength = 0;  //记录已下载的文件长度
            String downloadUrl = strings[0];    //获取下载url
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));  //下载文件名
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();    //下载目录
            file = new File(directory + fileName);
            if (file.exists()) {
                downloadedLength = file.length();   //如果文件已存在则读取下载的字节数
            }

            long contentLength = getContentLength(downloadUrl);     //获取文件总长度
            if (contentLength == 0) {
                return FAILED;  //如果文件长度为0，下载失败
            } else if (contentLength == downloadedLength) {
                return SUCCESS; //下载成功
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")  //断点下载，指定从哪个字节开始下载
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                inputStream = Objects.requireNonNull(response.body()).byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);   //跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = inputStream.read(b)) != -1) {
                    if (isCanceled) {
                        return CANCELED;
                    } else if (isPaused) {
                        return PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);
                        //计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        //通知下载进度
                        publishProgress(progress);
                    }
                }
                Objects.requireNonNull(response.body()).close();
                return SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (savedFile != null) {
                try {
                    savedFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isCanceled && file != null) {
                file.delete();
            }
        }

        return FAILED;
    }

    /**
     * 进度更新
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > mLastProgress) {
            mDownloadListener.onProgress(progress);     //回调下载进度
            mLastProgress = progress;
        }
    }

    /**
     * 根据参数传入的下载状态进行回调
     * @param integer
     */
    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case SUCCESS:
                mDownloadListener.onSuccess();
                break;
            case FAILED:
                mDownloadListener.onFailed();
                break;
            case PAUSED:
                mDownloadListener.onPaused();
                break;
            case CANCELED:
                mDownloadListener.onCanceled();
                break;
            default:
                break;
        }
    }

    /**
     * 暂停下载
     */
    public void pauseDownload() {
        isPaused = true;
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
