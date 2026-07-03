package com.district

import android.app.Application
import com.district.app.AppGraph

class DistrictApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
