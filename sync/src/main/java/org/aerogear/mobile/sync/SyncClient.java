package org.aerogear.mobile.sync;

import static org.aerogear.mobile.core.utils.SanityCheck.nonNull;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport.Factory;

import org.aerogear.mobile.core.MobileCore;
import org.aerogear.mobile.core.configuration.ServiceConfiguration;
import org.aerogear.mobile.core.executor.AppExecutors;
import org.aerogear.mobile.core.reactive.Request;
import org.aerogear.mobile.core.reactive.Requester;
import org.aerogear.mobile.core.reactive.Responder;

import okhttp3.OkHttpClient;

public final class SyncClient {

    public static final String TYPE = "sync";

    private static SyncClient instance;

    private final ApolloClient apolloClient;

    /**
     * Create service basing on existing builder
     *
     * @param builder ApolloClient.Builder
     */
    public SyncClient(ApolloClient.Builder builder) {
        MobileCore mobileCore = MobileCore.getInstance();
        ServiceConfiguration configuration = mobileCore.getServiceConfigurationByType(TYPE);
        String serverUrl = configuration.getUrl();
        OkHttpClient okHttpClient = mobileCore.getHttpLayer().getClient();
        builder.serverUrl(nonNull(serverUrl, "serverUrl"))
                        .okHttpClient(nonNull(okHttpClient, "okHttpClient"))
                        .subscriptionTransportFactory(new Factory(serverUrl, okHttpClient));
        apolloClient = builder.build();
    }

    /**
     * Default service with minimal required configuration
     */
    public SyncClient() {
        this(ApolloClient.builder());
    }

    /**
     * Get the SyncClient singleton instance
     *
     * @return SyncClient
     */
    public static SyncClient getInstance() {
        if (instance == null) {
            SyncClient.instance = new SyncClient();
        }
        return instance;
    }

    /**
     * Get underlying apollo client
     *
     * @return apolloClient
     */
    public ApolloClient getApolloClient() {
        return apolloClient;
    }

    /**
     * Apollo query wrapper using AeroGear Rx
     *
     * By default the respondOn of this will be on mainThread
     *
     * @param query Apollo Query instance
     * @return SyncQuery
     */
    public SyncQuery query(@Nonnull Query query) {
        return new SyncQuery(this.apolloClient, nonNull(query, "query"));
    }

    /**
     * Apollo mutation wrapper using AeroGear Rx
     *
     * By default the respondOn of this will be on mainThread
     *
     * @param mutation Apollo Mutation instance
     * @return SyncMutation
     */
    public SyncMutation mutation(@Nonnull Mutation mutation) {
        return new SyncMutation(this.apolloClient, nonNull(mutation, "mutation"));
    }

    /**
     * Apollo subscription wrapper using AeroGear Rx
     *
     * By default the respondOn of this will be on mainThread
     *
     * @param subscription Apollo Subscription instance
     * @return SyncSubscription
     */
    public SyncSubscription subscribe(@Nonnull Subscription subscription) {
        return new SyncSubscription(this.apolloClient, nonNull(subscription, "subscription"));
    }

    public static class SyncQuery {

        private final ApolloClient apolloClient;
        private final Query query;

        SyncQuery(ApolloClient apolloClient, Query query) {
            this.apolloClient = apolloClient;
            this.query = query;
        }

        public <T extends Operation.Data> Request<Response<T>> execute(
                        @Nonnull Class<T> responseDataClass) {

            nonNull(responseDataClass, "responseDataClass");

            return Requester.call((Responder<Response<T>> requestCallback) -> apolloClient
                            .query(query).enqueue(new ApolloCall.Callback<T>() {
                                @Override
                                public void onResponse(@Nonnull Response<T> response) {
                                    requestCallback.onResult(response);
                                }

                                @Override
                                public void onFailure(@Nonnull ApolloException e) {
                                    requestCallback.onException(e);
                                }
                            })).requestOn(new AppExecutors().networkThread())
                            .respondOn(new AppExecutors().mainThread());

        }

    }

    public static class SyncMutation {

        private final ApolloClient apolloClient;
        private final Mutation mutation;

        SyncMutation(ApolloClient apolloClient, Mutation mutation) {
            this.apolloClient = apolloClient;
            this.mutation = mutation;
        }

        public <T extends Operation.Data> Request<Response<T>> execute(
                        @Nonnull Class<T> responseDataClass) {

            nonNull(responseDataClass, "responseDataClass");

            return Requester.call((Responder<Response<T>> requestCallback) -> apolloClient
                            .mutate(mutation).enqueue(new ApolloCall.Callback<T>() {
                                @Override
                                public void onResponse(@Nonnull Response<T> response) {
                                    requestCallback.onResult(response);
                                }

                                @Override
                                public void onFailure(@Nonnull ApolloException e) {
                                    requestCallback.onException(e);
                                }
                            })).requestOn(new AppExecutors().networkThread())
                            .respondOn(new AppExecutors().mainThread());

        }
    }

    public static class SyncSubscription {

        private final ApolloClient apolloClient;
        private final Subscription subscription;

        SyncSubscription(ApolloClient apolloClient, Subscription subscription) {
            this.apolloClient = apolloClient;
            this.subscription = subscription;
        }

        public <T extends Operation.Data> Request<Response<T>> execute(
                        @Nonnull Class<T> responseDataClass) {

            nonNull(responseDataClass, "responseDataClass");

            return Requester.call((Responder<Response<T>> requestCallback) -> apolloClient
                            .subscribe(subscription).execute(new ApolloSubscriptionCall.Callback() {
                                @Override
                                public void onResponse(@NotNull Response response) {
                                    requestCallback.onResult(response);
                                }

                                @Override
                                public void onFailure(@NotNull ApolloException e) {
                                    requestCallback.onException(e);
                                }

                                @Override
                                public void onCompleted() {}
                            })).requestOn(new AppExecutors().networkThread())
                            .respondOn(new AppExecutors().mainThread());

        }
    }

}
