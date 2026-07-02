package com.district.jellyfinmono

import android.app.Application
import com.district.jellyfinmono.app.AppGraph

class DistrictApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
