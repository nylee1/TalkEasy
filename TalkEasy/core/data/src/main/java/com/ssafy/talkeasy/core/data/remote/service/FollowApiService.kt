package com.ssafy.talkeasy.core.data.remote.service

import com.ssafy.talkeasy.core.data.remote.datasource.common.DefaultResponse
import com.ssafy.talkeasy.core.data.remote.datasource.common.PagingDefaultResponse
import com.ssafy.talkeasy.core.data.remote.datasource.follow.AddFollowRequest
import com.ssafy.talkeasy.core.data.remote.datasource.follow.FollowResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FollowApiService {

    @GET("/api/follows")
    suspend fun requestFollowList(): PagingDefaultResponse<List<FollowResponse>>

    @POST("/api/follows/{toUserId}")
    suspend fun requestFollow(
        @Path("toUserId")
        toUserId: String,
        @Body
        body: AddFollowRequest,
    ): DefaultResponse<String>
}