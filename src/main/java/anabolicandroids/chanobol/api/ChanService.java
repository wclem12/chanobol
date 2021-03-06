package anabolicandroids.chanobol.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.api.data.Thread;

public class ChanService {
    Ion ion;
    Context cxt;
    Gson gson;

    public ChanService(Context context, Ion ion) {
        this.cxt = context;
        this.ion = ion;
        gson = new Gson();
    }

    public void listThreads(Object group, String board, final FutureCallback<List<Thread>> cb) {
        String path = String.format("/%s/catalog.json", board);
        ion.build(cxt)
                .load(ApiModule.endpoint + path)
                .group(group)
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override public void onCompleted(Exception e, JsonArray result) {
                        if (e != null) {
                            cb.onCompleted(e, null);
                            return;
                        }
                        List<Thread> threads = new ArrayList<>(50);
                        for (JsonElement x : result) {
                            for (JsonElement y : x.getAsJsonObject().get("threads").getAsJsonArray()) {
                                Thread t = gson.fromJson(y, Thread.class);

                                // Generate excerpt here instead of ThreadView for efficiency reasons
                                if (t.subject == null) t.subject = "";
                                if (t.text == null) t.text = "";
                                t.strippedSubject = android.text.Html.fromHtml(t.subject).toString();
                                String strippedText = android.text.Html.fromHtml(t.text).toString();
                                t.excerpt = t.strippedSubject
                                        + (t.strippedSubject.isEmpty() ? "" : "\n")
                                        + strippedText.substring(0, Math.min(160, strippedText.length()));

                                threads.add(t);
                            }
                        }
                        cb.onCompleted(null, threads);
                    }
                });
    }

    public void listPosts(Object group, String board, String number, final FutureCallback<List<Post>> cb) {
        String path = String.format("/%s/thread/%s.json", board, number);
        ion.build(cxt)
                .load(ApiModule.endpoint + path)
                .group(group)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            cb.onCompleted(e, null);
                            return;
                        }
                        Type type = new TypeToken<List<Post>>() {}.getType();
                        List<Post> posts = gson.fromJson(result.get("posts").getAsJsonArray(), type);
                        cb.onCompleted(null, posts);
                    }
                });
    }
}
