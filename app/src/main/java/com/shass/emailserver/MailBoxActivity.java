package com.shass.emailserver;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.shass.emailserver.Adapters.ConnectedAdapter;
import com.shass.emailserver.Adapters.MailAdapter;
import com.shass.emailserver.Tables.Connected;

public class MailBoxActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_box);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());



        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mail_box, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        final DBHelper _db = new DBHelper(getApplicationContext());
        switch (id) {
            //noinspection SimplifiableIfStatement
            case  (R.id.action_clear):
                _db.clearConnected();
                if (((RecyclerView)findViewById(R.id.rcView)).getAdapter()!=null)
                    ((ConnectedAdapter)((RecyclerView)findViewById(R.id.rcView)).getAdapter()).updateData(_db.getConnected());
                return true;
            case  (R.id.action_clear_mail):
                _db.clearMail();
                if (((RecyclerView)findViewById(R.id.rcView)).getAdapter()!=null)
                    ((MailAdapter)((RecyclerView)findViewById(R.id.rcView)).getAdapter()).updateData(_db.getMail());
                return true;
            case android.R.id.home:
                MailBoxActivity.this.finish();
                return true;
            }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_mail_box, container, false);


            final DBHelper db = new DBHelper(getActivity().getApplicationContext());
            final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.rcView);

            final SwipeRefreshLayout swipeLayout=(SwipeRefreshLayout) rootView.findViewById(R.id.swLayout);

            final int _position=getArguments().getInt(ARG_SECTION_NUMBER);

            switch (_position) {
                case 0:
                    final ConnectedAdapter connectedAdapter=new ConnectedAdapter(this.getContext(), db.getConnected());
                    recyclerView.setAdapter(connectedAdapter);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
                break;
                case 1:
                    final MailAdapter mailAdapter=new MailAdapter(this.getContext(), db.getMail());
                    recyclerView.setAdapter(new MailAdapter(this.getContext(), db.getMail()));
                    recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
                break;
            }
            db.close();

            swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    switch (_position) {
                        case 0:
                            ((ConnectedAdapter)recyclerView.getAdapter()).updateData(db.getConnected());
                            break;
                        case 1:
                            ((MailAdapter)recyclerView.getAdapter()).updateData(db.getMail());
                            break;
                    }
                    swipeLayout.setRefreshing(false);
                }
            });


            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position );
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return getString(R.string.title_connected);
                case 1:
                    return getString(R.string.title_mail);
            }
            return String.valueOf(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }
    }
}
