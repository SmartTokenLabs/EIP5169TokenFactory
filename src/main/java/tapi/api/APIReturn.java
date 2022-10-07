package tapi.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class APIReturn
{
    public APIReturn()
    {
        params = new ConcurrentHashMap<>();
    }

    void clear() { apiName = ""; params.clear(); }

    public String apiName;
    public final Map<String, String> params;
}