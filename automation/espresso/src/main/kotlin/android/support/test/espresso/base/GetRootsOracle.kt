package android.support.test.espresso.base

import android.os.Looper
import android.support.test.espresso.Root

object GetRootsOracle {
    private val rootsOracle = RootsOracle_Factory.newRootsOracle(Looper.getMainLooper())

    fun listActiveRoots(): List<Root> {
        return rootsOracle.listActiveRoots()
    }
}
