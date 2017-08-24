package pro.dbro.glance.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.loader.AsyncHttpRequestFactory;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pro.dbro.glance.R;
import pro.dbro.glance.adapters.AdapterUtils;
import pro.dbro.glance.adapters.ArticleAdapter;
import pro.dbro.glance.adapters.ReaderSectionAdapter;
import pro.dbro.glance.lib.SpritzerTextView;
import timber.log.Timber;

//import pro.dbro.glance.SECRETS;

public class FeedFragment extends ListFragment {

    ArrayAdapter<JsonObject> mFeedItemAdapter;
    ParseQueryAdapter<ParseObject> mArticleAdapter;
    //    ProgressBar mLoadingView;
    SpritzerTextView mLoadingView;

    // This "Future" tracks loading operations.
    Future<JsonObject> mFuture;

    private static final String ARG_FEED = "feed";
    private ReaderSectionAdapter.Feed mFeed;
    private static boolean sParseSetup = false;
    private boolean mLoading = false;

    public static FeedFragment newInstance(ReaderSectionAdapter.Feed feed) {
        FeedFragment f = new FeedFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_FEED, feed);
        f.setArguments(b);
        return f;
    }

    public void setupParse() {
        Parse.initialize(this.getActivity(), "IKXOwtsEGwpJxjD56rloizwwsB4pijEve8nU5wkB", "8K0yHwwEevmCiuuHTjGj7HRhFTzHmycBXXspmnPU");
        sParseSetup = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeed = (ReaderSectionAdapter.Feed) getArguments().getSerializable(ARG_FEED);
        if (!sParseSetup) {
            setupParse();
        }
    }

    public void onResume() {
        super.onResume();
        if (mLoading) showLoading();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myFragmentView = inflater.inflate(R.layout.fragment_list, container, false);
        ListView listView = (ListView) myFragmentView.findViewById(android.R.id.list);
//        mLoadingView = (ProgressBar) myFragmentView.findViewById(android.R.id.empty);
        mLoadingView = (SpritzerTextView) myFragmentView.findViewById(android.R.id.empty);

        switch (mFeed) {

            case POPULAR:
                mArticleAdapter = new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.RECENT);
                listView.setAdapter(mArticleAdapter);
                break;
            case RECENT:
                mArticleAdapter = new ArticleAdapter(getActivity(), ArticleAdapter.ArticleFilter.ALL);
                listView.setAdapter(mArticleAdapter);
                break;
            default:
                mFeedItemAdapter = createFeedAdapter();
                listView.setAdapter(mFeedItemAdapter);
                loadPipe(mFeed.getFeedUrl());
                break;
        }

        return myFragmentView;
    }

    // Create adapters from items coming from Pipes.
    private ArrayAdapter<JsonObject> createFeedAdapter() {
        return new ArrayAdapter<JsonObject>(getActivity(), 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.article_list_item, null);
                }

                JsonObject post = getItem(position);
                try {

                    String title = post.get("title").getAsString();
                    TextView handle = (TextView) convertView.findViewById(R.id.title);
                    handle.setText(title);

                    TextView text = (TextView) convertView.findViewById(R.id.url);
                    convertView.setTag((post.get("link").getAsString()));
                    try {
                        text.setText(new URL(post.get("link").getAsString()).getHost());
                    } catch (MalformedURLException e) {
                        text.setText(post.get("link").getAsString());
                    }

                    convertView.setOnClickListener(AdapterUtils.getArticleClickListener());
                    convertView.setOnLongClickListener(AdapterUtils.getArticleLongClickListener());
                } catch (Exception e) {
                    Timber.e(e, "Bind error");
                    // Parsing is fucked. NSFO.
                }

                return convertView;
            }
        };
    }

    private void loadPipe(String url) {
        // don't attempt to load more if a load is already in progress
        if (mFuture != null && !mFuture.isDone() && !mFuture.isCancelled()) return;

        mLoading = true;
        final AsyncHttpRequestFactory current = Ion.getDefault(getActivity()).configure().getAsyncHttpRequestFactory();
        Ion.getDefault(getActivity()).configure().setAsyncHttpRequestFactory(new AsyncHttpRequestFactory() {
            @Override
            public AsyncHttpRequest createAsyncHttpRequest(final Uri uri, final String method, final RawHeaders headers) {
                final AsyncHttpRequest request = current.createAsyncHttpRequest(uri, method, headers);
                request.setTimeout(10000);
                return request;
            }

        });

        final OkHttpClient build = new OkHttpClient.Builder().build();
        build.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showError();
                    }
                });
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {

                JsonReader reader = new JsonReader(new StringReader(response.body().string()));
                reader.setLenient(true);
                final JsonParser parser = new JsonParser();

                final JsonArray results = parser.parse(reader).getAsJsonArray();

                if (results.size() == 0) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showError();
                        }
                    });
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < results.size(); i++) {
                            mFeedItemAdapter.add(results.get(i).getAsJsonObject());
                        }
                    }
                });
            }
        });
    }

    private void showError() {
        mLoadingView.getSpritzer().setStaticText(getString(R.string.spritz_error));
    }

    private void showLoading() {
        mLoadingView.getSpritzer().setStaticText(getString(R.string.spritz_loading));
    }
}