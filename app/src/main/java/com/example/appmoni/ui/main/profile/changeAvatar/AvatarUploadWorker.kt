package com.example.appmoni.ui.main.profile.changeAvatar

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class AvatarUploadWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Lấy đường dẫn file ảnh dưới máy mà ProfileFragment gửi sang
        val localUriString = inputData.getString("LOCAL_URI") ?: return Result.failure()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            val uri = localUriString.toUri()
            val file = File(uri.path!!)

            // gọi api imgbb để upload
            val apiKey = "8eb28683fb2a6475aa9eab0854bbc81b"
            val client = OkHttpClient()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.Companion.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$apiKey")
                .post(requestBody)
                .build()

            // Thực thi gửi yêu cầu lên server
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody!!)
                val imageUrl = jsonObject.getJSONObject("data").getString("url")

                val data = hashMapOf("avatarUrl" to imageUrl)
                val dbTask = FirebaseFirestore.getInstance().collection("users").document(userId)
                    .set(data, SetOptions.merge())
                Tasks.await(dbTask)

                applicationContext.getSharedPreferences("MoniPrefs", Context.MODE_PRIVATE)
                    .edit { remove("pending_avatar") }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}