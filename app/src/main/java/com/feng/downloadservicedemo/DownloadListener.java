package com.feng.downloadservicedemo;

/**
 * 定义一个回调接口，用于对下载过程中的各种状态进行监听和回调
 * @author Feng Zhaohao
 * Created on 2018/9/24
 */
public interface DownloadListener {
    void onProgress(int progress);  //通知当前的下载进度
    void onSuccess();   //下载成功
    void onFailed();    //下载失败
    void onPaused();    //下载暂停
    void onCanceled();  //下载取消
}
