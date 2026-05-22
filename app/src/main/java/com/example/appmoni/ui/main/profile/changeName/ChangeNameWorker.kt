package com.example.appmoni.ui.main.profile.changeName

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChangeNameWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val newName = inputData.getString("NEW_NAME") ?: return Result.failure()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()

        return try {
            val data = hashMapOf("displayName" to newName)
            val dbTask = FirebaseFirestore.getInstance().collection("users").document(userId)
                .set(data, SetOptions.merge())

            Tasks.await(dbTask)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}