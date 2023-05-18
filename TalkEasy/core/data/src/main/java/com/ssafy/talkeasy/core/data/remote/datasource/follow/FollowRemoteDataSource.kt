package com.ssafy.talkeasy.core.data.remote.datasource.follow

import com.ssafy.talkeasy.core.data.remote.datasource.common.DefaultResponse
import com.ssafy.talkeasy.core.data.remote.datasource.common.PagingDefaultResponse
import com.ssafy.talkeasy.core.domain.entity.request.SosAlarmRequestBody

interface FollowRemoteDataSource {

    suspend fun requestFollowList(): PagingDefaultResponse<List<FollowResponse>>

    suspend fun requestNotificationList(): DefaultResponse<List<NotificationResponse>>

    suspend fun requestFollow(toUserId: String, body: AddFollowRequest): DefaultResponse<String>

    suspend fun requestSaveWardSOS(requestSosAlarmDto: SosAlarmRequestBody): DefaultResponse<String>
}