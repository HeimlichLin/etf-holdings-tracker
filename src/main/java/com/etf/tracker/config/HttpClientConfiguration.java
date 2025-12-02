package com.etf.tracker.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * HTTP 客戶端配置類別
 * <p>
 * 配置 OkHttp 客戶端連線池與逾時設定
 * </p>
 *
 * @author ETF Tracker Team
 * @version 1.0.0
 */
@Configuration
public class HttpClientConfiguration {

    private final AppConfig appConfig;

    public HttpClientConfiguration(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 建立 OkHttpClient Bean
     *
     * @return 配置完成的 OkHttpClient
     */
    @Bean
    public OkHttpClient okHttpClient() {
        AppConfig.HttpClientConfig config = appConfig.getHttpClient();

        // 建立連線池
        ConnectionPool connectionPool = new ConnectionPool(
                config.getMaxIdleConnections(),
                config.getKeepAliveDurationSeconds(),
                TimeUnit.SECONDS);

        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .connectTimeout(config.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(config.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .cookieJar(new CookieJar() {
                    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();
    }
}
