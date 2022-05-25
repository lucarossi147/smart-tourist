package io.github.lucarossi147.smarttourist

object Constants {
    const val AUTH_URL = "https://smart-tourist-cup3lszycq-uc.a.run.app/"
    const val POI_URL = "https://poi-service-container-cup3lszycq-uc.a.run.app/"
    const val ARG_USER = "user"
    const val MINIMUM_REFRESH_TIME = 1000 * 60 * 5
    const val ADD_VISIT_URL = AUTH_URL.plus("game/addVisit")
}