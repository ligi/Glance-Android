package pro.dbro.glance.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import pro.dbro.glance.fragments.FeedFragment;

public class ReaderSectionAdapter extends FragmentPagerAdapter {

    /**
     * A content feed.
     *
     * If a content feed is managed from a direct Parse query, it doesn't require a url.
     * See {@link pro.dbro.glance.fragments.FeedFragment#createFeedAdapter()} and
     * {@link pro.dbro.glance.adapters.ArticleAdapter}
    */
    public static enum Feed {
        POPULAR     ("Most Popular"),
        RECENT      ("Recent"),
        NEWS        ("News",       "https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/40805955111ac2e85631facfb362f067"),
        COMMENTARY  ("Commentary", "https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/dc1a399c275cbc0bcf6329c8419d6f4f"),
        FICTION     ("Fiction",    "https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/ee8d2db2513114660b054cd82da29b69"),
        HN          ("HN",         "https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/af38f38c0a21785ef8409d48ab4c1246"),
        TRUE_REDDIT ("True Reddit","https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/792a6a5fc2c23eafe6a80855263ac259"),
        DIGG        ("Digg",       "https://1yae9gfwab.execute-api.us-east-1.amazonaws.com/dev/8ef995083e5fe68ae3fe2d3d95f71844");

        private final String mTitle;
        private final String mFeedUrl;

        private Feed(final String title) {
            mTitle = title;
            mFeedUrl = null;
        }

        private Feed(final String title, final String url) {
            mTitle = title;
            mFeedUrl = url;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getFeedUrl() {
            return mFeedUrl;
        }
    }

    public ReaderSectionAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return Feed.values()[position].getTitle();
    }

    @Override
    public int getCount() {
        return Feed.values().length;
    }

    @Override
    public Fragment getItem(int position) {
        return FeedFragment.newInstance(Feed.values()[position]);
    }

}
