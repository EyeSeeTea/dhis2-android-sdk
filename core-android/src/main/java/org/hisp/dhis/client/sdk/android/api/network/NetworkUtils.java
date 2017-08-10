/*
 * Copyright (c) 2016, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.client.sdk.android.api.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hisp.dhis.client.sdk.core.common.Fields;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.core.common.network.ApiMessage;
import org.hisp.dhis.client.sdk.core.common.utils.CollectionUtils;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Response;

public class NetworkUtils {

    private NetworkUtils() {
        // no instances
    }

    @NonNull
    public static <T> List<T> getCollection(
            @NonNull ApiResource<T> apiResource, @NonNull Fields fields,
            @Nullable DateTime lastUpdated, @Nullable Set<String> uids) {
        return getCollection(apiResource, "id", fields, lastUpdated, uids);
    }

    @NonNull
    public static <T> List<T> getCollection(
            @NonNull ApiResource<T> apiResource, @NonNull String uidProperty,
            @NonNull Fields fields, @Nullable DateTime lastUpdated, @Nullable Set<String> uids) {

        Map<String, String> queryMap = new HashMap<>();
        List<String> filters = new ArrayList<>();

        /* disable paging */
        queryMap.put("paging", "false");

        /* filter programs by lastUpdated field */
        if (lastUpdated != null) {
            filters.add("lastUpdated:gt:" + lastUpdated.toString());
        }

        switch (fields) {
            case BASIC: {
                queryMap.put("fields", apiResource.getBasicProperties());
                break;
            }
            case ALL: {
                queryMap.put("fields", apiResource.getAllProperties());
                break;
            }
            case DESCENDANTS: {
                queryMap.put("fields", apiResource.getDescendantProperties());
                break;
            }
        }

        List<T> models = new ArrayList<>();
        if (uids != null && !uids.isEmpty()) {

            // splitting up request into chunks
            List<String> idFilters = buildIdFilter(uidProperty, uids);
            for (String idFilter : idFilters) {
                List<String> combinedFilters = new ArrayList<>(filters);
                combinedFilters.add(idFilter);

                // downloading subset of models
                models.addAll(unwrap(call(apiResource
                        .getEntities(queryMap, combinedFilters)), apiResource.getResourceName()));
            }
        } else {
            models.addAll(unwrap(call(

                    apiResource.getEntities(queryMap, filters)), apiResource.getResourceName()));
        }

        return models;
    }

    private static List<String> buildIdFilter(String uidProperty, Set<String> ids) {
        List<String> idFilters = new ArrayList<>();

        if (ids != null && !ids.isEmpty()) {
            List<List<String>> splittedIds = CollectionUtils.slice(new ArrayList<>(ids), 64);
            for (List<String> listOfIds : splittedIds) {
                StringBuilder builder = new StringBuilder();
                idFilters.add(builder
                        .append(uidProperty)
                        .append(":in:[")
                        .append(CollectionUtils.join(listOfIds, ","))
                        .append("]")
                        .toString());
            }
        }

        return idFilters;
    }

    @Nullable
    public static <T> T call(@NonNull Call<T> call) {
        Response<T> response = null;
        ApiException apiException = null;

        try {
            System.out.println(call.request().url().toString());
            System.out.println("Body" + bodyToString(call.request().body()));
            response = call.execute();
        } catch (IOException ioException) {
            System.out.println("Exception "+call.request().url().toString());
            ioException.printStackTrace();
            apiException = ApiException.networkError(null, ioException);
        }

        if (apiException != null) {
            throw apiException;
        }
        if (response.code() == 409) {
            System.out.println("Error Conflict: " + call.request().url().toString());
            try {
                String apiMessage = response.errorBody().string();
                System.out.println("error 409" + apiMessage);
                ObjectMapper mapper = new ObjectMapper();
                return (T) mapper.readValue(apiMessage, ApiMessage.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response.body();
        }
        if (!(response.code() >= 200 && response.code() < 300)) {
            System.out.println(
                    "Error !(>= 200 && <300)" + call.request().url().toString());
            try {
                String apiMessage = response.errorBody().string();
                System.out.println("error " + apiMessage);
                System.out.println(response.errorBody().string());
            } catch (IOException e) {
                e.printStackTrace();
            }

            throw ApiException.httpError(
                    response.raw().request().url().toString(),
                    ResponseMapper.fromRetrofitResponse(response));
        }

        return response.body();
    }

    private static String bodyToString(final RequestBody request) {
        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            if (copy != null) {
                copy.writeTo(buffer);
            } else {
                return "";
            }
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    @NonNull
    public static <T> List<T> unwrap(@Nullable Map<String, List<T>> response, @NonNull String key) {
        if (response != null && response.containsKey(key) && response.get(key) != null) {
            return response.get(key);
        } else {
            return new ArrayList<>();
        }
    }

    public byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();
        } catch (OutOfMemoryError error) {
            return null;
        }
    }
}
