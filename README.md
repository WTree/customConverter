把Retrofi 和Okhttp相关的依赖加上

如:(具体要看最新版本)
```
    implementation 'androidx.recyclerview:recyclerview:1.0.0'
    implementation 'com.squareup.okhttp3:okhttp:3.6.0'
    implementation 'com.squareup.retrofit2:retrofit:2.3.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
    implementation 'com.squareup.retrofit2:adapter-rxjava2:2.3.0'
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.0'
```

Step 1. Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.WTree:customConverter:v0.9'
	}



在Retrofit 的初始化时加上

  addConverterFactory(MyGsonConverterFactory.create())

  ```
  如果需要登录检查的注解使用需要实现
    LoginCheckHelper.getInstance().setCallback(object :ILoginCheckCallback{
            override fun loginJump() {
                if(LoginHelper.isLogin(App.getInstance())){
                    LoginHelper.clearLogin(App.getInstance())
                }
                LoginActivity.newLaunch(App.getInstance())
            }

            override fun isNeedLogin(jsonObj: JSONObject?): Boolean {
                var code = jsonObj?.getInt("code")
                return code == 10001
            }

        })

       
    然后在对应的实体加上
    @LoginCheck

    ```
    IJsonParse 如果实体实现这个接口是自己手动解析Json 

    
