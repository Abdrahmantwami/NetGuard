package eu.faircode.netguard.monitor;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

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
                    .registerTypeAdapter(Scan.class, new ScanTypeAdapter()).create()))
            .client(client)
            .build();


    public static MetaDefenderAPI getMetaDefenderAPI() {
        return retrofit.create(MetaDefenderAPI.class);
    }

    private static class ScanTypeAdapter extends TypeAdapter<Scan> {
        private Gson mGson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy
                .LOWER_CASE_WITH_UNDERSCORES).create();

        @Override public void write(final JsonWriter out, final Scan value) throws IOException {
            throw new IOException("Scan Type Not support to write to json");
        }

        @Override public Scan read(final JsonReader in) throws IOException {
            ScanQueryResult scanQueryResult = mGson.fromJson(in, ScanQueryResult.class);
            return new Scan(scanQueryResult);
        }
    }
}
