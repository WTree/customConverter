package com.common.http.JSONFactory;

import android.text.TextUtils;

import com.common.http.LoginCheckHelper;
import com.common.http.LoginException;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * 这个类大部分的是复用GsonConverterFactory 的方法
 * 有些接口无法建模的
 * 在解析 时，通过反射或者动态代理来解析json
 * 实现这个目的就是为了有些坑爹的后端返回的json 很乱
 * 默认是GsonConverterFactory 的具体实现
 * Created by WTree on 2018/4/10.
 */

public class MyGsonConverterFactory extends Converter.Factory {


    static String CONVERT_METHOD = "convert";
    static String CONVERT_PARSE = "parse";

    /**
     * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    public static MyGsonConverterFactory create() {
        return create(new Gson());
    }

    /**
     * Create an instance using {@code gson} for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    public static MyGsonConverterFactory create(Gson gson) {
        return new MyGsonConverterFactory(gson);
    }

    private final Gson gson;

    private MyGsonConverterFactory(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        this.gson = gson;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                            Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return proxyNewInstance(gson, adapter, type);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return newInstanceGsonRequestBodyConverter(gson, adapter);
    }


    /**
     * 动态代理实现
     *
     * @param gson
     * @param adapter
     * @param <T>
     * @return
     */
    static <T> Converter<ResponseBody, T> proxyNewInstance(Gson gson, TypeAdapter<T> adapter, Type type) {
        try {
            Class<?> cls = Class.forName("retrofit2.Converter");
            Converter<ResponseBody, T> obj = newInstanceGsonResponseBodyConverter(gson, adapter, type);
            Converter<ResponseBody, T> proxy = (Converter<ResponseBody, T>) Proxy.newProxyInstance(cls.getClassLoader(),
                    new Class[]{cls},
                    new AutoCoverHOOK(obj, type,gson));
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * 判断model 是否有实现
     *
     * @param <T>
     */
    static class AutoCoverHOOK<T> implements InvocationHandler {

        Converter<ResponseBody, T> mBaseObj;
        private boolean isNeedParse;
        private Class<?> cls;
        private boolean isNeedRemoveHead = false;
        String dataFields = "data";
        Type type;
        private boolean isNeedLoginCheck;
        Gson gson;

        public AutoCoverHOOK(Converter<ResponseBody, T> mBaseObj, Type type, Gson gson) {
            this.mBaseObj = mBaseObj;
            this.type = type;
            this.gson = gson;
            this.isNeedParse = type instanceof Class<?> && (IJsonParse.class.isAssignableFrom((Class<?>) type));
            if (isNeedParse) {
                cls = (Class<?>) type;
            }
            if (type instanceof Class<?>) {
                Annotation ani = ((Class) type).getAnnotation(NOHead.class);


                if (ani != null) {

                    NOHead noHead = ((Class<?>) type).getAnnotation(NOHead.class);
                    if (noHead != null) {
                        dataFields = noHead.value();
                    }

                    cls = (Class<?>) type;
                    isNeedRemoveHead = true;


                }

                ani = ((Class) type).getAnnotation(LoginCheck.class);
                if (ani != null) {
                    LoginCheck loginCheck = ((Class<?>) type).getAnnotation(LoginCheck.class);
                    if (loginCheck != null) {
                        cls = (Class<?>) type;
                        isNeedLoginCheck = true;
                    }
                }


            } else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type temp = parameterizedType.getActualTypeArguments()[0];
                if (temp instanceof Class<?>) {
                    Annotation ani = ((Class) temp).getAnnotation(NOHead.class);
                    if (ani != null) {
                        NOHead noHead = ((Class<?>) temp).getAnnotation(NOHead.class);
                        if (noHead != null) {
                            dataFields = noHead.value();
                        }

                        cls = (Class<?>) temp;
                        isNeedRemoveHead = true;
                    }
                    ani = ((Class) temp).getAnnotation(LoginCheck.class);

                    if (ani != null) {

                        LoginCheck loginCheck = ((Class<?>) temp).getAnnotation(LoginCheck.class);
                        if (loginCheck != null) {
                            cls = (Class<?>) temp;
                            isNeedLoginCheck = true;
                        }
                    }

                }
            }
            if (isNeedParse) {
                cls = (Class<?>) type;
            }


        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (CONVERT_METHOD.equals(method.getName()) && cls != null) {

                ResponseBody value = (ResponseBody) args[0];

                String json = value.string();
                value.close();
                if (isNeedLoginCheck) {

                    try {
                        JSONObject jsonObject=new JSONObject(json);
                        if( LoginCheckHelper.isNeedLogin(jsonObject)){
                            throw new LoginException("需要重新登录");
                        }

                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }





                if (isNeedParse) {

                    return parse(json, cls);
                } else if (isNeedRemoveHead) {
                    return parseNoHeader(json, gson, type, dataFields);
                } else {
                    return parseOther(json, gson, type);
                }

            }
            return method.invoke(mBaseObj, args);
        }
    }

    /**
     * 还没有调通
     *
     * @param body
     * @param mapper
     * @param type
     * @param dataFields
     * @return
     * @throws Exception
     */
    private static Object parseNoHeader(String json, Gson gson, Type type, String dataFields) throws Exception {

        dataFields = TextUtils.isEmpty(dataFields) ? "data" : dataFields;
        if (TextUtils.isEmpty(json)) {
            throw new RuntimeException("json is empty");
        }
        JSONObject jsonObject = new JSONObject(json);
        if (!jsonObject.isNull(dataFields)) {
            Object obj = jsonObject.opt(dataFields);

            String noHeadJson = obj.toString();
            return gson.fromJson(noHeadJson, type);


        }
        return json;


    }

    private static Object parseOther(String json, Gson gson, Type type) throws Exception {


        return gson.fromJson(json, type);


    }

    private static Object parse(String json, Class<?> cls) throws Exception {

        Object obj = cls.newInstance();
        try {
            Method method = cls.getMethod(CONVERT_PARSE, String.class);
            method.setAccessible(true);
            return method.invoke(obj, json);

        } finally {
        }

    }


    /**
     * return new GsonResponseBodyConverter<>(gson, adapter);
     *
     * @param gson
     * @param adapter
     * @param <T>
     * @return
     */
    static <T> Converter<ResponseBody, T> newInstanceGsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter, Type type) {
        try {
//           Class<?> cls=Class.forName("retrofit2.converter.jackson.bo")
            Class<T> cls = (Class<T>) Class.forName("retrofit2.converter.gson.GsonResponseBodyConverter");
            Constructor constructor = cls.getDeclaredConstructor(Gson.class, TypeAdapter.class);//获取有参构造
            constructor.setAccessible(true);
            Object obj = constructor.newInstance(gson, adapter);
            return (Converter<ResponseBody, T>) obj;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * new GsonRequestBodyConverter<>(gson, adapter);
     *
     * @param gson
     * @param adapter
     * @param <T>
     * @return
     */
    static <T> Converter<T, RequestBody> newInstanceGsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {

        try {
            Class<?> cls = Class.forName("retrofit2.converter.gson.GsonRequestBodyConverter");

            Constructor constructor = cls.getDeclaredConstructor(Gson.class, TypeAdapter.class);//获取有参构造
            constructor.setAccessible(true);
            Object obj = constructor.newInstance(gson, adapter);
            return (Converter<T, RequestBody>) obj;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
