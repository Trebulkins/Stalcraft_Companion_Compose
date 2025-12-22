package com.example.stalcraft_companion_compose.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class VersionInfo(
    @Expose @SerializedName("updated_at") val updatedAt: String
) : Parcelable
