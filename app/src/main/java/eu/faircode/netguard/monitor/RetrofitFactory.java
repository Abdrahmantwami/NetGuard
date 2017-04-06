package eu.faircode.netguard.monitor;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Carlos on 4/6/17.
 */

public class RetrofitFactory {
    static final OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(new
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build();

    static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.metadefender.com/v2/")
            .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()))
            .client(client)
            .build();


    public static MetaDefenderAPI getMetaDefenderAPI() {
        return retrofit.create(MetaDefenderAPI.class);
    }
}
