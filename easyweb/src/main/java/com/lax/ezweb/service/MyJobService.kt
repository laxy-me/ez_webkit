package com.lax.ezweb.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi

/**
 *
 * @author yangguangda
 * @date 2020/5/6
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class MyJobService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        return false
    }
}