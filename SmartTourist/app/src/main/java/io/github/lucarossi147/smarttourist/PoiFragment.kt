package io.github.lucarossi147.smarttourist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import io.github.lucarossi147.smarttourist.Constants.ADD_VISIT_URL
import io.github.lucarossi147.smarttourist.Constants.ARG_USER
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.POI
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ARG_POI = "poi"

/**
 * A simple [Fragment] subclass.
 */
class PoiFragment : Fragment() {
    private var poi: POI? = null
    private var user: LoggedInUser? = null
    private var signEditText: EditText? = null
    private var signButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            poi = it.getParcelable(ARG_POI)
            user = it.getParcelable(ARG_USER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_poi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signButton = view.findViewById(R.id.signButton)
        signEditText = view.findViewById(R.id.editTextTextSignYourself)
        if (poi?.visited != null && poi?.visited == false) {
            signEditText?.visibility = View.VISIBLE
            signButton?.visibility = View.VISIBLE
        }
        val nonNullUser = user?:return
        val nonNullPoi = poi?:return

        val tv: TextView = view.findViewById(R.id.poiInfoTextView)
        tv.text = poi?.info

        signButton?.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val jsonObject = JsonObject()
                jsonObject.addProperty("idPoi", nonNullPoi.id)
                jsonObject.addProperty("signature", signEditText?.text.toString())
                val res = HttpClient(Android)
                    .post(ADD_VISIT_URL){
                        contentType(ContentType.Application.Json)
                        setBody(jsonObject.toString())
                        bearerAuth(nonNullUser.token)
                    }
                CoroutineScope(Dispatchers.Main).launch {
                    if (res.status.isSuccess()){
                        nonNullUser.visitedPois  = nonNullUser.visitedPois + nonNullPoi.id
                        signEditText?.visibility = View.GONE
                        signButton?.visibility = View.GONE
                    } else {
                        Toast.makeText(context, "Sign was not successful", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (nonNullPoi.id in nonNullUser.visitedPois){
            signEditText?.visibility = View.GONE
            signButton?.visibility = View.GONE
        }

        val goToSignatureButton: Button = view.findViewById(R.id.goToSignaturesButton)
        goToSignatureButton.setOnClickListener {
            view.findNavController().navigate(R.id.signaturesFragment, bundleOf(ARG_USER to user, "poiId" to poi!!.id))
        }
        val backToMapButton: Button = view.findViewById(R.id.backToMapButton)
        backToMapButton.setOnClickListener {
            view.findNavController().navigate(R.id.mapsFragment, bundleOf(ARG_USER to user))
        }
        poi?.pictures?.forEach {
            val iv = ImageView(context)
            val linearLayout: LinearLayout = view.findViewById(R.id.images_linear_layout)
            linearLayout.addView(iv)
            Picasso.get()
                .load(it)
                .resize(0, 400)
                .into(iv)
        }


    }
}