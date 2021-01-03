package com.common.http.JSONFactory;


import androidx.annotation.WorkerThread;

import org.json.JSONException;

/**
 * Created by WTree on 2018/4/12.
 *
 * 继承这个必须要有一个默认的构造函数
 *
 */

public interface IJsonParse {
    @WorkerThread
    Object parse(String json) throws JSONException;
}
