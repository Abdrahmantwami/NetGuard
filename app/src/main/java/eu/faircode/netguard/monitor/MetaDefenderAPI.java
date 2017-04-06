package eu.faircode.netguard.monitor;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

/**
 * Created by Carlos on 4/4/17.
 */

public interface MetaDefenderAPI {

    @GET("hash/{hash_value}")
    @Headers({"apikey: 2fdb7380227857ba340639ca4b6cd934", "Accept-Encoding: identity"})
    Call<ScanQueryResult> hashLookUp(@Path("hash_value") String hash);

}
