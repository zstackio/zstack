package org.zstack.externalStorage.sdk;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import org.apache.poi.ss.formula.functions.T;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

/**
 * @ Author : yh.w
 * @ Date   : Created in 15:14 2024/5/14
 */
public abstract class ExternalStorageApiClient {
    private static final CLogger logger = Utils.getLogger(ExternalStorageApiClient.class);
    public static OkHttpClient http = new OkHttpClient();

    public static final Gson gson;
    public static final DateTimeFormatter formatter;

    public static final long ACTION_DEFAULT_TIMEOUT = -1;
    public static final long ACTION_DEFAULT_POLLINGINTERVAL = -1;

    static {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss VV")
                .toFormatter(Locale.ENGLISH);
    }
}
