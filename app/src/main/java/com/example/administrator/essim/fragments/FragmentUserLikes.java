package com.example.administrator.essim.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.administrator.essim.R;
import com.example.administrator.essim.activities.UserDetailActivity;
import com.example.administrator.essim.activities.ViewPagerActivity;
import com.example.administrator.essim.adapters.AuthorWorksAdapter;
import com.example.administrator.essim.interf.OnItemClickListener;
import com.example.administrator.essim.interf.RefreshLayout;
import com.example.administrator.essim.network.AppApiPixivService;
import com.example.administrator.essim.network.RestClient;
import com.example.administrator.essim.response.IllustsBean;
import com.example.administrator.essim.response.Reference;
import com.example.administrator.essim.response.UserIllustsResponse;
import com.example.administrator.essim.utils.Common;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

import static android.view.View.VISIBLE;

public class FragmentUserLikes extends ScrollObservableFragment {

    public static int dataType;
    public static RefreshLayout sRefreshLayout;
    private String next_url;
    private Context mContext;
    private TextView mTextView;
    private RecyclerView rcvGoodsList;
    private AuthorWorksAdapter mPixivAdapterGrid;
    private SharedPreferences mSharedPreferences;
    private int scrolledY = 0;
    private List<IllustsBean> mIllustsBeanList = new ArrayList<>();

    public static FragmentUserLikes newInstance() {
        FragmentUserLikes fragment = new FragmentUserLikes();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();
        mSharedPreferences = Common.getLocalDataSet();
        View v = inflater.inflate(R.layout.fragment_home_list, container, false);
        initView(v);
        getLikeIllust("public");
        return v;
    }

    private void initView(View v) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mPixivAdapterGrid.getItemViewType(position) == 3 ||
                        mPixivAdapterGrid.getItemViewType(position) == 2) {
                    return gridLayoutManager.getSpanCount();
                } else {
                    return 1;
                }
            }
        });
        rcvGoodsList = v.findViewById(R.id.rcvGoodsList);
        rcvGoodsList.setLayoutManager(gridLayoutManager);
        rcvGoodsList.setHasFixedSize(true);
        rcvGoodsList.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                scrolledY += dy;
                if (FragmentUserLikes.this.isResumed()) {
                    doOnScrollChanged(0, scrolledY, dx, dy);
                }
            }
        });
        mTextView = v.findViewById(R.id.post_like_user);
    }

    private void getLikeIllust(String starType) {
        FragmentUserDetail.mShowProgress.showProgress(true);
        dataType = starType.equals("public") ? 0 : 1;
        Call<UserIllustsResponse> call = new RestClient()
                .getRetrofit_AppAPI()
                .create(AppApiPixivService.class)
                .getLikeIllust(mSharedPreferences.getString("Authorization", ""), ((UserDetailActivity) getActivity()).getUserId(), starType);
        call.enqueue(new retrofit2.Callback<UserIllustsResponse>() {
            @Override
            public void onResponse(Call<UserIllustsResponse> call, retrofit2.Response<UserIllustsResponse> response) {
                if(getView() != null) {
                    if (response.body().getIllusts().size() == 0) {
                        // 没有数据，recyclerview不显示，显示textview提示
                        FragmentUserDetail.mShowProgress.showProgress(false);
                        if (rcvGoodsList.getVisibility() == VISIBLE) {
                            rcvGoodsList.setVisibility(View.INVISIBLE);
                        }
                        if (mTextView.getVisibility() == View.INVISIBLE) {
                            mTextView.setText("这里空空的，什么也没有~");
                            mTextView.setVisibility(VISIBLE);
                        }
                    } else {
                        UserIllustsResponse userIllustsResponse = response.body();
                        mIllustsBeanList.clear();
                        mIllustsBeanList.addAll(userIllustsResponse.getIllusts());
                        mPixivAdapterGrid = new AuthorWorksAdapter(mIllustsBeanList, mContext);
                        next_url = userIllustsResponse.getNext_url();
                        mPixivAdapterGrid.setOnItemClickListener(new OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position, int viewType) {
                                if (position == -1) {
                                    if (next_url != null) {
                                        getNextUserIllust();
                                    } else {
                                        Snackbar.make(rcvGoodsList, "没有更多数据了", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                    }
                                } else if (viewType == 0) {
                                    Reference.sIllustsBeans = mIllustsBeanList;
                                    Intent intent = new Intent(mContext, ViewPagerActivity.class);
                                    intent.putExtra("which one is selected", position);
                                    mContext.startActivity(intent);
                                } else if (viewType == 1) {
                                    if (!mIllustsBeanList.get(position).isIs_bookmarked()) {
                                        ((ImageView) view).setImageResource(R.drawable.ic_favorite_white_24dp);
                                        view.startAnimation(Common.getAnimation());
                                        Common.postStarIllust(position, mIllustsBeanList,
                                                mSharedPreferences.getString("Authorization", ""), mContext, "public");
                                    } else {
                                        ((ImageView) view).setImageResource(R.drawable.ic_favorite_border_black_24dp);
                                        view.startAnimation(Common.getAnimation());
                                        Common.postUnstarIllust(position, mIllustsBeanList,
                                                mSharedPreferences.getString("Authorization", ""), mContext);
                                    }
                                }
                            }

                            @Override
                            public void onItemLongClick(View view, int position) {
                                if (!mIllustsBeanList.get(position).isIs_bookmarked()) {
                                    ((ImageView) view).setImageResource(R.drawable.ic_favorite_white_24dp);
                                    Common.postStarIllust(position, mIllustsBeanList,
                                            mSharedPreferences.getString("Authorization", ""), mContext, "private");
                                }
                            }
                        });
                        // 有数据，textview不显示，显示recyclerview
                        if (rcvGoodsList.getVisibility() == View.INVISIBLE) {
                            rcvGoodsList.setVisibility(VISIBLE);
                        }
                        if (mTextView.getVisibility() == VISIBLE) {
                            mTextView.setVisibility(View.INVISIBLE);
                        }
                        FragmentUserDetail.mShowProgress.showProgress(false);
                        rcvGoodsList.setAdapter(mPixivAdapterGrid);
                        scrolledY = 0;
                        rcvGoodsList.scrollBy(0, FragmentUserDetail.scrollYset);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserIllustsResponse> call, Throwable throwable) {

            }
        });
    }

    private void getNextUserIllust() {
        FragmentUserDetail.mShowProgress.showProgress(true);
        Call<UserIllustsResponse> call = new RestClient()
                .getRetrofit_AppAPI()
                .create(AppApiPixivService.class)
                .getNextUserIllusts(mSharedPreferences.getString("Authorization", ""), next_url);
        call.enqueue(new retrofit2.Callback<UserIllustsResponse>() {
            @Override
            public void onResponse(Call<UserIllustsResponse> call, retrofit2.Response<UserIllustsResponse> response) {
                UserIllustsResponse userIllustsResponse = response.body();
                next_url = userIllustsResponse.getNext_url();
                mIllustsBeanList.addAll(userIllustsResponse.getIllusts());
                FragmentUserDetail.mShowProgress.showProgress(false);
                mPixivAdapterGrid.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<UserIllustsResponse> call, Throwable throwable) {

            }
        });
    }

    @Override
    public void setScrolledY(int scrolledY) {
        if (rcvGoodsList != null) {
            if (this.scrolledY >= scrolledY) {
                int scrollDistance = (this.scrolledY - scrolledY) * -1;
                rcvGoodsList.scrollBy(0, scrollDistance);
            } else {
                rcvGoodsList.scrollBy(0, scrolledY);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPixivAdapterGrid != null) {
            mPixivAdapterGrid.notifyDataSetChanged();
        }
        sRefreshLayout = this::getLikeIllust;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sRefreshLayout = null;
    }
}
