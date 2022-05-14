package io.github.lucarossi147.smarttourist

import org.junit.Test
import kotlin.properties.Delegates

class ObservableTest {

    private class TestObservable {
        var changeThis = ""
        var url:String by Delegates.observable(""){
                _, _, newValue ->
            changeThis = "changed!"
        }
    }

    @Test
    fun testObservable() {
        val observable = TestObservable()
        assert(observable.changeThis == "")
        observable.url = "smart-tourist.com"
        assert(observable.changeThis == "changed!")
    }
}